package core.grandexchange;

import core.base.SimpleNode;
import core.config.ScriptConfig;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.grandexchange.GrandExchangeItem;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.walking.web.node.impl.items.RequiredItem;
import org.dreambot.api.methods.walking.web.node.impl.teleports.TeleportWebNode;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.container.impl.bank.BankMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Node for handling Grand Exchange operations for restocking
 * Integrates with RestockGrandExchangeManager to handle restocking
 */
public class RestockGrandExchangeNode extends SimpleNode<ScriptConfig> {
    private static final Area GRAND_EXCHANGE_AREA = new Area(3143, 3507, 3186, 3471);

    private final RestockCheckUtility restockCheck;

    // Add common teleport items that should be recognized
    private static final String[] COMMON_TELEPORT_ITEMS = {
        "Ring of wealth (5)", "Ring of wealth (4)", "Ring of wealth (3)", 
        "Ring of wealth (2)", "Ring of wealth (1)", "Varrock teleport"
    };

    private enum State {
        CHECK_BANK,
        WALK_TO_GE,
        HANDLE_GE,
        RETURN_TO_BANK,
        FINISHED,
        CHECK_TELEPORTS, IDLE
    }

    private State currentState = State.IDLE;
    private boolean forceExecute = false;
    private boolean restockInProgress = false;
    private int restockAttempts = 0;
    private static final int MAX_RESTOCK_ATTEMPTS = 3;

    private long lastStateChangeTime = 0;
    private static final long STATE_TIMEOUT = 60000; // 60 seconds timeout

    // Track GE operations
    private boolean sellOperationsCompleted = false;
    private boolean buyOperationsCompleted = false;
    private int geOperationAttempts = 0;
    private static final int MAX_GE_OPERATION_ATTEMPTS = 3;
    private static final Tile GE_CENTER = GRAND_EXCHANGE_AREA.getCenter();

    // Pending lists for GE operations
    private List<String> pendingSellItems = new ArrayList<>();
    private List<String> pendingBuyItems = new ArrayList<>();
    private Map<String, Integer> pendingBuyQuantities = new java.util.HashMap<>();

    private boolean buyListInitialized = false;

    // --- GE Buy Offer Logic ---
    private final Map<String, BuyOffer> activeBuyOffers = new java.util.HashMap<>();

    /**
     * Constructor
     *
     * @param name     The node name
     * @param priority The node priority
     * @param config   The script config
     */
    public RestockGrandExchangeNode(String name, int priority, ScriptConfig config) {
        super(name, priority, config);
        this.restockCheck = new RestockCheckUtility(config);
    }

    public void forceExecute() {
        this.forceExecute = true;
    }

    @Override
    public boolean validate() {

        // If we're forcing execution, validate regardless of state
        if (forceExecute) {
            Logger.info("RestockGrandExchangeNode validating due to forceExecute");
            forceExecute = false;
            restockInProgress = true;
            setState(State.CHECK_BANK);
            return true;
        }

        // Only allow a new restock check if IDLE or FINISHED and bank is open
        if ((currentState == State.IDLE || currentState == State.FINISHED) && Bank.isOpen() && !restockInProgress) {
            Logger.info("[RestockGE] Checking if restock is needed (bank is open)");
            if (restockCheck.needsRestock()) {
                Logger.info("Restock needed, starting restock process");
                restockInProgress = true;
                restockAttempts++;
                setState(State.CHECK_BANK);
                return true;
            } else {
                Logger.info("No restock needed, all items are at or above minimum thresholds");
                return false;
            }
        }

        // If we're already in an active state, continue execution
        if (currentState != State.IDLE && currentState != State.FINISHED) {
            Logger.info("RestockGrandExchangeNode validating due to active state: " + currentState);
            return true;
        }

        // Check if we've been in the same state for too long (safety mechanism)
        if (currentState != State.IDLE && currentState != State.FINISHED &&
                System.currentTimeMillis() - lastStateChangeTime > STATE_TIMEOUT) {
            Logger.warn("[RestockGE] State timeout reached in state " + currentState + ". Resetting to prevent stuck condition.");
            reset();
            return false;
        }

        // Check if we've exceeded max attempts
        if (restockAttempts >= MAX_RESTOCK_ATTEMPTS) {
            Logger.warn("[RestockGE] Reached maximum restock attempts (" + MAX_RESTOCK_ATTEMPTS + "). Resetting node.");
            reset();
            return false;
        }

        return false;
    }

    private void setState(State newState) {
        currentState = newState;
        lastStateChangeTime = System.currentTimeMillis();

        // Reset operation flags when entering GE state
        if (newState == State.HANDLE_GE) {
            sellOperationsCompleted = false;
            buyOperationsCompleted = false;
            geOperationAttempts = 0;
            buyListInitialized = false;
        }

        // Clear restock flag when finished
        if (newState == State.FINISHED) {
            restockInProgress = false;
        }

        Logger.info("[RestockGE] State changed to: " + newState);
    }

    private boolean hasRequiredItems(List<RequiredItem> items) {
        for (RequiredItem item : items) {
            if (Bank.count(item.getName()) < item.getCount()) {
                return false;
            }
        }
        return true;
    }

    private boolean isTabTeleport(List<RequiredItem> items) {
        return items.size() == 1 && items.get(0).getName().toLowerCase().contains("teleport");
    }

    @Override
    protected int onExecute() {
        try {
            Logger.info("[RestockGE] onExecute called, currentState: " + currentState);

            switch (currentState) {
                case CHECK_BANK:
                    return handleCheckBank();
                case CHECK_TELEPORTS:
                    if (!Bank.isOpen() && !BankLocation.GRAND_EXCHANGE.getArea(15).contains(Players.getLocal())) {
                        BankLocation nearestBank = Bank.getClosestBankLocation();
                        if (nearestBank != null && !nearestBank.getArea(8).contains(getLocalPlayer())) {
                            Logger.log("[GE Node] Walking to nearest bank: " + Bank.getClosestBankLocation());
                            Walking.walk(nearestBank.getArea(0).getTile());
                            Sleep.sleepUntil(() -> nearestBank.getArea(8).contains(getLocalPlayer()), 5000);
                            return 600;
                        }
                        Logger.log("[GE Node] Opening bank...");
                        Bank.open();
                        Sleep.sleepUntil(Bank::isOpen, 5000);
                        Bank.depositAllExcept("Coins");
                        return 600;
                    }

                    // Check for explicit teleport items first
                    boolean foundTeleport = false;
                    
                    for (String teleportItem : COMMON_TELEPORT_ITEMS) {
                        if (Bank.count(teleportItem) > 0) {
                            Logger.info("[GE Node] Found teleport item in bank: " + teleportItem);
                            Bank.withdraw(teleportItem, 1);
                            Sleep.sleepUntil(() -> Inventory.contains(teleportItem), 3000);
                            foundTeleport = true;
                            break;
                        }
                    }
                    
                    if (foundTeleport) {
                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                        setState(State.WALK_TO_GE);
                        return 600;
                    }

                    // Fall back to DreamBot's teleport logic 
                    // Find the best teleport option
                    List<TeleportWebNode> teleportNodes = TeleportWebNode.getTeleportNodes();
                    TeleportWebNode bestTeleport = null;
                    double bestDistance = Double.MAX_VALUE;
                    List<RequiredItem> bestRequiredItems = null;
                    boolean foundTab = false;

                    for (TeleportWebNode teleportNode : teleportNodes) {
                        double distanceToGE = teleportNode.getTile().distance(GE_CENTER);
                        List<RequiredItem> requiredItems = teleportNode.getRequiredItems();

                        if (!requiredItems.isEmpty() && hasRequiredItems(requiredItems)) {
                            boolean isTab = isTabTeleport(requiredItems);

                            // If we haven't found a tab yet, or this is a tab and closer, or we have no teleport yet
                            if ((!foundTab && isTab) ||
                                    (isTab && foundTab && distanceToGE < bestDistance) ||
                                    (bestTeleport == null) ||
                                    (!foundTab && !isTab && distanceToGE < bestDistance)) {

                                bestDistance = distanceToGE;
                                bestTeleport = teleportNode;
                                bestRequiredItems = requiredItems;
                                foundTab = isTab;
                            }
                        }
                    }

                    // If we found a good teleport option, withdraw the items
                    if (bestTeleport != null && bestRequiredItems != null) {
                        Logger.log("[GE Node] Selected best teleport with distance " + bestDistance + " tiles to GE");
                        Logger.log("[GE Node] Withdrawing required items: " + bestRequiredItems.stream()
                                .map(item -> item.getName() + " (x" + item.getCount() + ")")
                                .collect(Collectors.joining(", ")));

                        for (RequiredItem item : bestRequiredItems) {
                            Logger.log("[GE Node] Withdrawing: " + item.getName() + " x" + item.getCount());
                            Bank.withdraw(item.getName(), item.getCount());
                            Sleep.sleepUntil(() -> Inventory.count(item.getName()) >= item.getCount(), 3000);
                        }

                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                        setState(State.WALK_TO_GE);
                        return 600;
                    } else {
                        Logger.log("[GE Node] No suitable teleports found, proceeding with normal walking");
                        Bank.close();
                        setState(State.WALK_TO_GE);
                        return 600;
                    }
                case WALK_TO_GE:
                    return handleWalkToGE();
                case HANDLE_GE:
                    return handleGrandExchange();
                case RETURN_TO_BANK:
                    return handleReturnToBank();
                case FINISHED:
                    Logger.info("[RestockGE] Restock process complete.");
                    // Deposit purchased items into bank before reset
                    if (Bank.isOpen()) {
                        for (String item : config.getRestockConfig().getInventoryItemsToRestock()) {
                            if (!config.getRestockConfig().isSelling(item)) {
                                int invCount = Inventory.count(item);
                                if (invCount > 0) {
                                    Logger.info("[RestockGE] Depositing " + invCount + "x " + item + " into bank");
                                    Bank.depositAll(item);
                                    Sleep.sleepUntil(() -> Inventory.count(item) == 0, 3000);
                                }
                            }
                        }
                        Logger.info("[RestockGE] Closing bank after deposit");
                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                    }
                    reset();
                    return 1000;
                case IDLE:
                    Logger.info("[RestockGE] Node is idle.");
                    return 1000;
                default:
                    Logger.warn("[RestockGE] Unknown state: " + currentState);
                    reset();
                    return 1000;
            }
        } catch (Exception e) {
            Logger.error("Error in RestockGrandExchangeNode: " + e.getMessage(), e);
            reset();
            return 1000;
        }
    }

    private int handleCheckBank() {
        // Open bank if needed
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 3000);
                return 600;
            }
            return 1000;
        }

        // Get lists of items to sell/buy
        List<String> itemsToSell = restockCheck.getItemsToSell();
        Map<String, Integer> itemsToBuy = restockCheck.getItemsToBuy();

        // Store pending lists for GE processing
        pendingSellItems.clear();
        pendingSellItems.addAll(itemsToSell);
        pendingBuyItems.clear();
        pendingBuyItems.addAll(itemsToBuy.keySet());
        pendingBuyQuantities.clear();
        pendingBuyQuantities.putAll(itemsToBuy);
        Logger.info("[RestockGE][DEBUG] pendingBuyItems: " + pendingBuyItems);
        Logger.info("[RestockGE][DEBUG] pendingBuyQuantities: " + pendingBuyQuantities);

        Logger.info("[RestockGE] Items to sell: " + (itemsToSell.isEmpty() ? "(none)" : String.join(", ", itemsToSell)));
        Logger.info("[RestockGE] Items to buy: " + (itemsToBuy.isEmpty() ? "(none)" : String.join(", ", itemsToBuy.keySet())));

        if (itemsToSell.isEmpty() && itemsToBuy.isEmpty()) {
            setState(State.FINISHED);
            return 0;
        }

        // Withdraw items to sell as notes for GE
        if (!itemsToSell.isEmpty() && Bank.getWithdrawMode() != BankMode.NOTE) {
            Bank.setWithdrawMode(BankMode.NOTE);
        }
        boolean withdrawnAny = false;
        for (String sellItem : itemsToSell) {
            int sellQty = config.getRestockConfig().getSellQuantity(sellItem);
            int bankQty = Bank.count(sellItem);
            if (sellQty > 0 && bankQty > 0) {
                int withdrawQty = Math.min(sellQty, bankQty);
                if (Bank.withdraw(sellItem, withdrawQty)) {
                    Sleep.sleepUntil(() -> Inventory.count(sellItem) >= withdrawQty, 3000);
                    withdrawnAny = true;
                }
            }
        }

        // Calculate & withdraw coins for buying
        int totalCoinsNeeded = 0;
        for (Map.Entry<String, Integer> entry : itemsToBuy.entrySet()) {
            String buyItem = entry.getKey();
            int buyQty = entry.getValue();
            if (buyQty > 0) {
                int itemCost = calculateBuyCost(buyItem, buyQty);
                Logger.info("[RestockGE] Item: " + buyItem + " - Buy qty: " + buyQty + " - Estimated cost: " + itemCost);
                totalCoinsNeeded += itemCost;
            }
        }
        
        if (totalCoinsNeeded > 0) {
            int bankCoins = Bank.count("Coins");
            Logger.info("[RestockGE] Need " + totalCoinsNeeded + " coins for buys. Bank has: " + bankCoins);
            
            if (bankCoins >= totalCoinsNeeded) {
                // Create a final copy of the variable for use in lambda
                final int coinAmount = totalCoinsNeeded;
                if (Bank.withdraw("Coins", coinAmount)) {
                    Sleep.sleepUntil(() -> Inventory.count("Coins") >= coinAmount, 3000);
                    withdrawnAny = true;
                } else {
                    Logger.warn("[RestockGE] Failed to withdraw coins");
                }
            } else {
                Logger.warn("[RestockGE] Not enough coins to buy all items. Needed: " + totalCoinsNeeded + ", have: " + bankCoins);
                // Continue with what we have
                if (bankCoins > 0) {
                    // Create a final copy for the lambda
                    final int availableCoins = bankCoins;
                    if (Bank.withdraw("Coins", availableCoins)) {
                        Sleep.sleepUntil(() -> Inventory.count("Coins") > 0, 3000);
                        withdrawnAny = true;
                    } else {
                        Logger.warn("[RestockGE] Failed to withdraw available coins");
                    }
                }
            }
        }

        // Close bank and move to next state
        Bank.close();
        Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
        
        if (withdrawnAny) {
            Logger.info("[RestockGE] Items withdrawn for GE. Moving to GE.");
            setState(State.CHECK_TELEPORTS);
        } else {
            Logger.info("[RestockGE] Nothing to withdraw. Finishing.");
            setState(State.FINISHED);
        }
        
        return 600;
    }

    private int handleWalkToGE() {
        if (GRAND_EXCHANGE_AREA.contains(getLocalPlayer())) {
            setState(State.HANDLE_GE);
            return 0;
        }

        // Check if we have a teleport in inventory first
        if (tryUseInventoryTeleport()) {
            return 2000; // Wait for teleport animation
        }
        
        // Use walking as a fallback
        if (Walking.shouldWalk()) {
            Walking.walk(GRAND_EXCHANGE_AREA.getCenter());
            return 1000;
        }
        return 600;
    }

    // New method to try teleporting with inventory items
    private boolean tryUseInventoryTeleport() {
        Logger.info("[GE Node] Checking inventory for teleport items...");
        
        for (String teleportItem : COMMON_TELEPORT_ITEMS) {
            if (Inventory.contains(teleportItem)) {
                Logger.info("[GE Node] Found teleport item in inventory: " + teleportItem);
                
                if (teleportItem.contains("Ring of wealth")) {
                    Logger.info("[GE Node] Using Ring of wealth teleport to Grand Exchange");
                    if (Inventory.interact(teleportItem, "Grand Exchange")) {
                        Sleep.sleepUntil(() -> GRAND_EXCHANGE_AREA.contains(getLocalPlayer()), 8000);
                        return true;
                    }
                } else if (teleportItem.equals("Varrock teleport")) {
                    Logger.info("[GE Node] Using Varrock teleport");
                    if (Inventory.interact(teleportItem, "Break")) {
                        Sleep.sleepUntil(() -> !Inventory.contains(teleportItem), 8000);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private int handleGrandExchange() {
        // Increment attempt counter
        geOperationAttempts++;
        if (geOperationAttempts > MAX_GE_OPERATION_ATTEMPTS) {
            setState(State.RETURN_TO_BANK);
            return 0;
        }

        // Open GE interface if needed
        if (!GrandExchange.isOpen()) {
            if (GrandExchange.open()) {
                Sleep.sleepUntil(GrandExchange::isOpen, 3000);
                return 600;
            }
            return 1000;
        }

        // Handle selling using pending list
        if (!sellOperationsCompleted) {
            List<String> itemsToSell = new ArrayList<>(pendingSellItems);
            Logger.info("[RestockGE] Items to sell: " + (itemsToSell.isEmpty() ? "none" : String.join(", ", itemsToSell)));
            if (itemsToSell.isEmpty()) {
                sellOperationsCompleted = true;
            } else {
                sellOperationsCompleted = processSellOperations(itemsToSell);
                if (!sellOperationsCompleted) {
                    return 1000;
                }
            }
        }

        // Only initialize buy list ONCE per session, using the list from handleCheckBank
        if (!buyOperationsCompleted) {
            if (!buyListInitialized) {
                // Prepare buy list for this session
                activeBuyOffers.clear();
                for (String itemName : pendingBuyItems) {
                    int buyQty = pendingBuyQuantities.getOrDefault(itemName, 0);
                    Logger.info("[RestockGE][DEBUG] Adding to GE buy list: " + itemName + " x" + buyQty);
                    if (buyQty > 0) {
                        activeBuyOffers.put(itemName, new BuyOffer(itemName, buyQty));
                    } else {
                        Logger.warn("[RestockGE][DEBUG] Skipping addItemToBuy for " + itemName + " due to zero quantity");
                    }
                }
                buyListInitialized = true;
            }
            buyOperationsCompleted = handleBuyOffers();
            if (!buyOperationsCompleted) {
                return 1000;
            }
        }

        // After all operations, collect and finish
        if (GrandExchange.isReadyToCollect()) {
            GrandExchange.collect();
            Sleep.sleep(1000, 1500);
            // Always return to bank after collecting
            setState(State.RETURN_TO_BANK);
            return 0;
        }
        if (GrandExchange.isOpen()) {
            GrandExchange.close();
            Sleep.sleepUntil(() -> !GrandExchange.isOpen(), 3000);
        }
        setState(State.RETURN_TO_BANK);
        return 0;
    }
    
    /**
     * Processes all sell operations for the given list of items
     * @param itemsToSell list of item names to sell from inventory
     * @return True if all sell offers were placed or no further action needed, false if retry required
     */
    private boolean processSellOperations(List<String> itemsToSell) {
        boolean allOffersPlaced = true;
        boolean anyOfferPlaced = false;

        Logger.info("[RestockGE] Beginning sell operations for " + itemsToSell.size() + " items");

        for (String itemName : itemsToSell) {
            int desiredSellQty = config.getRestockConfig().getSellQuantity(itemName);
            int inventoryQty = Inventory.all(i -> i.getName().equals(itemName)).stream()
                                    .mapToInt(i -> i.getAmount())
                                    .sum();
            int qtyToSell = Math.min(inventoryQty, desiredSellQty);

            if (qtyToSell <= 0) {
                Logger.info("[RestockGE] No " + itemName + " in inventory to sell");
                continue;
            }

            // Find a wrapper to sell (prefer noted if available)
            var wrappers = Inventory.all(i -> i.getName().equals(itemName));
            if (wrappers.isEmpty()) {
                Logger.warn("[RestockGE] Could not find " + itemName + " in inventory to sell");
                allOffersPlaced = false;
                continue;
            }
            var wrapper = wrappers.get(0);

            int price = calculateSellPrice(itemName);
            Logger.info(String.format("[RestockGE] Placing sell offer: %dx %s at %d gp ea", qtyToSell, itemName, price));
            boolean success = GrandExchange.sellItem(wrapper.getID(), qtyToSell, price);
            if (success) {
                anyOfferPlaced = true;
                Sleep.sleep(1200, 1800);
            } else {
                Logger.warn("[RestockGE] Failed to place sell offer for " + itemName);
                allOffersPlaced = false;
            }
        }

        if (anyOfferPlaced) {
            // Collect completed sell offers
            Sleep.sleep(3000, 4000);
            while (GrandExchange.isReadyToCollect()) {
                Logger.info("[RestockGE] Collecting sell offers...");
                GrandExchange.collect();
                Sleep.sleep(800, 1200);
            }
        }

        return allOffersPlaced;
    }

    private int handleReturnToBank() {
        if (BankLocation.GRAND_EXCHANGE.getArea(8).contains(getLocalPlayer())) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 3000);
                Bank.depositAllItems();
                restockInProgress = false;
                setState(State.FINISHED);
                return 0;
            }
            return 600;
        }
        
        if (Walking.shouldWalk()) {
            Walking.walk(BankLocation.GRAND_EXCHANGE.getCenter());
            return 1000;
        }
        return 600;
    }

    /**
     * Resets the node state
     */
    public void reset() {
        Logger.info("[RestockGE] Resetting node state");
        currentState = State.IDLE;
        forceExecute = false;
        restockInProgress = false;
        lastStateChangeTime = 0;
        sellOperationsCompleted = false;
        buyOperationsCompleted = false;
        geOperationAttempts = 0;
        restockAttempts = 0;
        pendingSellItems.clear();
        pendingBuyItems.clear();
        buyListInitialized = false;
        pendingBuyQuantities.clear();
        activeBuyOffers.clear();
    }

    /**
     * Checks if the node is finished
     * @return True if finished, false otherwise
     */
    public boolean isFinished() {
        return !restockInProgress && (currentState == State.IDLE || currentState == State.FINISHED);
    }

    /**
     * Forces a restock check
     */
    public void forceRestockCheck() {
        Logger.info("Forcing restock check...");
        forceExecute = true;
        restockInProgress = true;
        restockAttempts = 0;
        setState(State.CHECK_BANK);
    }

    public boolean isForceExecuting() {
        return forceExecute;
    }

    /**
     * Gets the RestockCheckUtility used by this node
     * @return The RestockCheckUtility instance
     */
    public RestockCheckUtility getRestockCheck() {
        return restockCheck;
    }

    /**
     * Calculates an appropriate sell price for an item based on GE market value
     * @param itemName The name of the item
     * @return The calculated sell price
     */
    private int calculateSellPrice(String itemName) {
        int marketPrice;
        try {
            marketPrice = org.dreambot.api.methods.grandexchange.LivePrices.get(itemName);
            Logger.info("[RestockGE] Got market price for " + itemName + ": " + marketPrice);
        } catch (Exception e) {
            Logger.warn("[RestockGE] Error getting price for " + itemName + ": " + e.getMessage());
            marketPrice = 0;
        }
        
        if (marketPrice <= 0) {
            Logger.warn("[RestockGE] Could not get price for " + itemName + ", using default value of 100");
            return 100; // Default fallback value
        }
        
        // Use different sell strategies based on the value of the item
        double sellMultiplier;
        if (marketPrice > 10000) {
            // For valuable items, sell closer to market price to ensure it sells
            sellMultiplier = 0.97;
        } else if (marketPrice > 1000) {
            // For medium value items
            sellMultiplier = 0.95;
        } else {
            // For cheap items, go lower to ensure they sell quickly
            sellMultiplier = 0.90;
        }
        
        int sellPrice = (int)(marketPrice * sellMultiplier);
        Logger.info("[RestockGE] Calculated sell price for " + itemName + ": " + sellPrice + 
                   " (" + (sellMultiplier * 100) + "% of market price: " + marketPrice + ")");
                   
        return Math.max(sellPrice, 1); // Ensure at least 1gp
    }

    // --- GE Buy Offer Logic ---
    private boolean handleBuyOffers() {
        if (!GrandExchange.isOpen()) {
            if (!GrandExchange.open()) {
                Logger.warn("[RestockGE] Failed to open Grand Exchange");
                return false;
            }
            Sleep.sleepUntil(GrandExchange::isOpen, 5000);
            return false;
        }

        // Place buy offers for all items in activeBuyOffers
        int availableSlots = 8 - GrandExchange.getUsedSlots();
        int placed = 0;
        for (BuyOffer offer : new ArrayList<>(activeBuyOffers.values())) {
            if (placed >= availableSlots) break;
            if (offer.placed) continue;
            int price = getBuyPrice(offer.itemName);
            if (price <= 0) {
                Logger.warn("[RestockGE] Could not get price for " + offer.itemName + ", skipping");
                continue;
            }
            int slot = GrandExchange.getFirstOpenSlot();
            if (slot != -1 && GrandExchange.buyItem(offer.itemName, offer.quantity, price)) {
                Logger.info("[RestockGE] Placed buy offer: " + offer.quantity + "x " + offer.itemName + " @ " + price + " gp");
                offer.placed = true;
                offer.slot = slot;
                placed++;
                Sleep.sleep(800, 1200);
            } else {
                Logger.warn("[RestockGE] Failed to place buy offer for " + offer.itemName);
            }
        }

        // Collect completed offers
        boolean allComplete = true;
        for (BuyOffer offer : activeBuyOffers.values()) {
            if (!offer.placed) {
                allComplete = false;
                continue;
            }
            GrandExchangeItem geItem = getOfferBySlot(offer.slot);
            if (geItem != null && geItem.isReadyToCollect()) {
                Logger.info("[RestockGE] Collecting completed offer for " + offer.itemName);
                GrandExchange.collect();
                Sleep.sleep(800, 1200);
            }
            // Check if item is in inventory or bank
            if (Inventory.count(offer.itemName) >= offer.quantity || Bank.count(offer.itemName) >= offer.quantity) {
                offer.completed = true;
            } else {
                allComplete = false;
            }
        }

        // Remove completed offers
        activeBuyOffers.values().removeIf(o -> o.completed);

        // If all offers are complete, return true
        return activeBuyOffers.isEmpty();
    }

    private GrandExchangeItem getOfferBySlot(int slot) {
        GrandExchangeItem[] geItems = GrandExchange.getItems();
        if (geItems != null && slot >= 0 && slot < geItems.length) {
            return geItems[slot];
        }
        return null;
    }

    private int getBuyPrice(String itemName) {
        int marketPrice = 0;
        try {
            marketPrice = LivePrices.get(itemName);
        } catch (Exception e) {
            Logger.warn("[RestockGE] Error getting price for " + itemName + ": " + e.getMessage());
        }
        if (marketPrice <= 0) return 1000;
        double multiplier = 1.25 + (Math.random() * 0.15); // 1.25x to 1.4x
        return (int) (marketPrice * multiplier);
    }

    private int calculateBuyCost(String itemName, int quantity) {
        int price = getBuyPrice(itemName);
        return price * quantity;
    }

    private static class BuyOffer {
        final String itemName;
        final int quantity;
        boolean placed = false;
        boolean completed = false;
        int slot = -1;
        BuyOffer(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }
}

