package scripts.ScarWraths.muling;

import core.base.SimpleNode;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.trade.Trade;
import org.dreambot.api.methods.trade.TradeUser;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.World;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.message.Message;
import scripts.ScarWraths.ScarWrathsScript;
import scripts.ScarWraths.config.ScarWrathsConfig;
import core.muling.MuleClient;
import core.muling.SocketMuleClient;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles muling for the ScarWraths script
 */
public class MulingNode extends SimpleNode<ScarWrathsConfig> {
    private final ScarWrathsScript script;
    private final MuleClient muleClient;
    private MuleStatus status = MuleStatus.IDLE;
    private long lastStatusChangeTime = 0;
    private final long STATUS_TIMEOUT = 180_000; // 3 minutes timeout for most states

    // Define meeting areas
    private static final Area VARROCK_MID = new Area(3207, 3432, 3253, 3420); // Varrock center area (expanded to include fountain area)
    private static final Area FEROX_ENCLAVE_BANK = new Area(3134, 3632, 3139, 3627); // Ferox Enclave bank area
    private static final Area GRAND_EXCHANGE_AREA = new Area(3158, 3494, 3171, 3483); // Grand Exchange area
    private Area currentMeetingArea = GRAND_EXCHANGE_AREA; // Default to GE
    private static final BankLocation BANK_LOCATION = BankLocation.VARROCK_EAST;

    // Constants
    private static final String WRATH_RUNE = "Wrath rune";
    private static final String NATURE_RUNE = "Nature rune";

    public MulingNode(ScarWrathsConfig config, ScarWrathsScript script) {
        super("Muling", 0, config); // Highest priority (0)
        this.script = script;
        this.muleClient = new SocketMuleClient(config.getMulingConfig());
        updateMeetingArea();
    }

    /**
     * Update the meeting area based on the current configuration
     */
    private void updateMeetingArea() {
        String meetingSpot = config.getMulingConfig().getMeetingSpot();
        if (meetingSpot.equalsIgnoreCase("Grand Exchange")) {
            currentMeetingArea = GRAND_EXCHANGE_AREA;
            Logger.log("MulingNode: Meeting area set to Grand Exchange");
        } else if (meetingSpot.equalsIgnoreCase("Ferox Enclave")) {
            currentMeetingArea = FEROX_ENCLAVE_BANK;
            Logger.log("MulingNode: Meeting area set to Ferox Enclave");
        } else if (meetingSpot.equalsIgnoreCase("Varrock Mid") ||
                   meetingSpot.equalsIgnoreCase("Varrock")) {
            currentMeetingArea = BankLocation.VARROCK_EAST.getArea(5);
            Logger.log("MulingNode: Meeting area set to Varrock Mid");
        } else {
            // Default to Grand Exchange if unknown
            currentMeetingArea = GRAND_EXCHANGE_AREA;
            Logger.warn("MulingNode: Unknown meeting spot '" + meetingSpot + "', defaulting to Grand Exchange.");
        }
    }

    /**
     * Set the muling status and track the time of the change
     */
    private void setStatus(MuleStatus newStatus) {
        if (this.status != newStatus) {
            Logger.log("Mule Status: " + this.status + " -> " + newStatus);
            this.status = newStatus;
            this.lastStatusChangeTime = System.currentTimeMillis();
        }
    }

    /**
     * Check if the current status has timed out
     */
    private boolean hasTimedOut() {
        if (status == MuleStatus.IDLE || status == MuleStatus.CHECK_CONDITIONS || status == MuleStatus.COMPLETE) {
            return false; // No timeout for these states
        }
        boolean timedOut = System.currentTimeMillis() - lastStatusChangeTime > STATUS_TIMEOUT;
        if (timedOut) {
            Logger.warn("MuleStatus " + status + " timed out after " + (STATUS_TIMEOUT / 1000) + " seconds.");
            setStatus(MuleStatus.ERROR);
        }
        return timedOut;
    }

    @Override
    public boolean validate() {
        if (!config.getMulingConfig().isEnabled()) {
            if (status != MuleStatus.IDLE) {
                // If muling was active but now disabled, clean up
                cleanupMuleConnection();
                setStatus(MuleStatus.IDLE);
            }
            return false;
        }

        // If already in a muling process, this node should run
        if (status != MuleStatus.IDLE && status != MuleStatus.CHECK_CONDITIONS) {
            return true;
        }

        // Get the rune type to check based on script configuration
        String runeType = config.getRuneTypeEnum() == ScarWrathsConfig.RuneType.WRATH ? WRATH_RUNE : NATURE_RUNE;

        // Check for runes to mule
        List<String> itemsToMule = config.getMulingConfig().getItemsToMule();
        boolean itemsConfigured = itemsToMule != null && !itemsToMule.isEmpty() && itemsToMule.contains(runeType);

        if (!itemsConfigured) {
            Logger.log("MulingNode Debug: " + runeType + " not configured for muling");
            return false;
        }

        // Get trigger value
        int triggerValue = config.getMulingConfig().getTriggerValue();

        // Check inventory count
        int inventoryRunes = Inventory.count(runeType);
        Logger.log("MulingNode Debug: Inventory " + runeType + ": " + inventoryRunes + ", Trigger value: " + triggerValue);

        // Check bank count (even if bank is not open)
        int bankRunes = Bank.count(runeType);
        Logger.log("MulingNode Debug: Bank " + runeType + ": " + bankRunes);

        // Calculate total runes (inventory + bank)
        int totalRunes = inventoryRunes + bankRunes;
        Logger.log("MulingNode Debug: Total " + runeType + " (inventory + bank): " + totalRunes);

        // Check if we meet the threshold with either inventory alone or total count
        if (inventoryRunes >= triggerValue) {
            Logger.log("MulingNode: Inventory " + runeType + " threshold met! Starting muling process");
            setStatus(MuleStatus.CHECK_CONDITIONS);
            return true;
        }

        if (totalRunes >= triggerValue) {
            Logger.log("MulingNode: Total " + runeType + " threshold met! Starting muling process");
            setStatus(MuleStatus.CHECK_CONDITIONS);
            return true;
        }

        return false;
    }

    @Override
    protected int onExecute() {
        if (hasTimedOut()) {
            handleError("Muling state timed out.");
            return 1000;
        }

        updateMeetingArea(); // Ensure meeting area is up-to-date

        switch (status) {
            case CHECK_CONDITIONS:
                return handleCheckConditions();
            case WALKING_TO_MEET:
                return handleWalkingToMeet();
            case HOPPING_WORLD:
                return handleHoppingWorld();
            case CONNECTING_TO_MULE:
                return handleConnecting();
            case WAITING_FOR_MULE:
                return handleWaitingForMule();
            case TRADING:
                return handleTrading();
            case TRADE_ACCEPTED_NEED_CONFIRM:
                return handleWaitingForMuleConfirm();
            case WAITING_FOR_COMPLETION:
                return handleWaitForCompletion();
            case COMPLETE:
                return handleComplete();
            case DISCONNECTING:
                return handleDisconnecting();
            case ERROR:
                return handleError("Muling process encountered an error."); // Fallback error handling
            case IDLE:
            default:
                // Should not happen if validate() is correct, but good to have a default
                setStatus(MuleStatus.IDLE);
                return 1000;
        }
    }

    private int handleCheckConditions() {
        // Get the rune type to check based on script configuration
        String runeType = config.getRuneTypeEnum() == ScarWrathsConfig.RuneType.WRATH ? WRATH_RUNE : NATURE_RUNE;

        // Double-check conditions before proceeding
        int inventoryRunes = Inventory.count(runeType);
        int bankRunes = Bank.count(runeType);
        int totalRunes = inventoryRunes + bankRunes;
        int triggerValue = config.getMulingConfig().getTriggerValue();
        List<String> itemsToMule = config.getMulingConfig().getItemsToMule();
        boolean itemsConfigured = itemsToMule != null && !itemsToMule.isEmpty() && itemsToMule.contains(runeType);

        Logger.log("MulingNode: Checking conditions - Inventory " + runeType + ": " + inventoryRunes +
                  ", Bank " + runeType + ": " + bankRunes + ", Trigger value: " + triggerValue);

        // If we don't have enough in inventory but have enough in bank, we need to withdraw
        if (inventoryRunes < triggerValue && bankRunes > 0 && totalRunes >= triggerValue) {
            // Need to withdraw from bank
            // Walk to bank if not already there
            if (!Bank.isOpen() && !BANK_LOCATION.getArea(10).contains(Players.getLocal())) {
                Logger.info("Walking to Varrock East bank");
                if (Walking.walk(BANK_LOCATION.getCenter())) {
                    Sleep.sleepUntil(() -> BANK_LOCATION.getArea(10).contains(Players.getLocal()), 1600);
                }
                return 600;
            }
            if (!Bank.isOpen()) {
                Logger.log("MulingNode: Opening bank to withdraw " + runeType);
                if (Bank.open()) {
                    Sleep.sleepUntil(Bank::isOpen, 5000);
                } else {
                    Logger.error("MulingNode: Failed to open bank");
                    setStatus(MuleStatus.IDLE);
                    return 1000;
                }
                return 600;
            }

            // Bank is open, withdraw runes
            int amountToWithdraw = Math.min(bankRunes, triggerValue - inventoryRunes);
            Logger.log("MulingNode: Withdrawing " + amountToWithdraw + " " + runeType);

            if (Bank.withdraw(runeType, amountToWithdraw)) {
                Sleep.sleepUntil(() -> Inventory.count(runeType) >= triggerValue, 3000);
                inventoryRunes = Inventory.count(runeType); // Update count
                Logger.log("MulingNode: Now have " + inventoryRunes + " " + runeType + " in inventory");
            } else {
                Logger.error("MulingNode: Failed to withdraw " + runeType);
                setStatus(MuleStatus.IDLE);
                return 1000;
            }
        }

        // Check if we have enough to mule now
        boolean hasEnoughRunes = inventoryRunes >= triggerValue;

        if (hasEnoughRunes && itemsConfigured) {
            // Close bank if open
            if (Bank.isOpen()) {
                Logger.log("MulingNode: Closing bank before starting mule process.");
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                return 600;
            }
            // Proceed to next step
            Logger.log("MulingNode: Conditions verified. Starting muling process.");
            setStatus(MuleStatus.WALKING_TO_MEET);
            return 100;
        } else {
            // Conditions no longer met
            Logger.log("MulingNode: Conditions no longer met. Returning to IDLE.");
            setStatus(MuleStatus.IDLE);
            return 500;
        }
    }

    private int handleWalkingToMeet() {
        // Make sure meeting area is up-to-date
        updateMeetingArea();

        // First check if we're already at the meeting area
        if (currentMeetingArea.contains(Players.getLocal())) {
            Logger.log("MulingNode: Already at meeting area: " + config.getMulingConfig().getMeetingSpot());
            setStatus(MuleStatus.HOPPING_WORLD);
            return 100;
        }

        // Log our current position and the meeting area for debugging
        Tile playerTile = Players.getLocal().getTile();
        Tile targetTile = currentMeetingArea.getCenter();
        int distance = (int) playerTile.distance(targetTile);

        Logger.log("MulingNode: Current position: " + playerTile);
        Logger.log("MulingNode: Target position: " + targetTile + " (distance: " + distance + ")");
        Logger.log("MulingNode: Meeting area: " + currentMeetingArea);
        Logger.log("MulingNode: Walking to " + config.getMulingConfig().getMeetingSpot() + "...");

        // Always try to walk if we're not in the area
        if (!currentMeetingArea.contains(Players.getLocal())) {
            // Always use regular walking since web walker might not be available
            if (Walking.walk(targetTile)) {
                Logger.log("MulingNode: Walking to center of meeting area: " + targetTile);
                Sleep.sleepUntil(() -> currentMeetingArea.contains(Players.getLocal()) || !Players.getLocal().isMoving(), 8000, 600);
            } else {
                Logger.warn("MulingNode: Failed to start walking to meeting area");
            }
        }

        // Double-check if we've arrived
        if (currentMeetingArea.contains(Players.getLocal())) {
            Logger.log("MulingNode: Arrived at meeting area.");
            setStatus(MuleStatus.HOPPING_WORLD);
            return 100;
        }

        // Check if we're stuck
        if (Players.getLocal().isStandingStill()) {
            Logger.log("MulingNode: Player is standing still, trying to walk again");
            Walking.walk(targetTile);
        }

        // Return a short sleep time to keep trying to walk
        return 300; // Shorter sleep time to retry walking more frequently
    }

    private int handleHoppingWorld() {
        int targetWorld = config.getMulingConfig().getMuleWorld();
        int currentWorld = Worlds.getCurrentWorld();

        if (currentWorld == targetWorld) {
            Logger.log("MulingNode: Already in target world " + targetWorld);
            setStatus(MuleStatus.CONNECTING_TO_MULE);
            return 100;
        }

        // Check if world exists and is available
        World world = Worlds.getWorld(targetWorld);
        if (world == null) {
            return handleError("Target world " + targetWorld + " does not exist");
        }


        Logger.log("Hopping to world " + targetWorld + "...");
        if (WorldHopper.hopWorld(targetWorld)) {
            Logger.log("MulingNode: Hop initiated, waiting for world change...");
            if (Sleep.sleepUntil(() -> Worlds.getCurrentWorld() == targetWorld, 15000, 1000)) {
                Logger.log("MulingNode: Successfully hopped to world " + targetWorld);

                // Add stabilization delay after world hop before attempting to connect
                Logger.log("MulingNode: Adding stabilization delay after world hop (3 seconds)...");
                Sleep.sleep(3000);

                setStatus(MuleStatus.CONNECTING_TO_MULE);
                return 100;
            } else {
                Logger.error("MulingNode: Failed to hop to world " + targetWorld);
                return handleError("Failed to hop to world " + targetWorld);
            }
        } else {
            Logger.error("MulingNode: Failed to initiate hop to world " + targetWorld);
            return handleError("Failed to initiate hop to world " + targetWorld);
        }
    }

    private int handleConnecting() {
        Logger.log("Connecting to mule...");

        // First check if we're in the correct world
        int targetWorld = config.getMulingConfig().getMuleWorld();
        if (Worlds.getCurrentWorld() != targetWorld) {
            Logger.error("MulingNode: World mismatch detected before connection attempt. Current: "
                + Worlds.getCurrentWorld() + ", Target: " + targetWorld);

            // If we're somehow in the wrong world, go back to hopping
            setStatus(MuleStatus.HOPPING_WORLD);
            return 100;
        }

        // Verify settings first
        if (!verifyMuleSettings()) {
            return handleError("Mule settings verification failed - check logs for details");
        }

        // Check if we're already connected first
        if (muleClient.isConnected()) {
            Logger.log("MulingNode: Already connected to mule server");

            // Send our username as the trader (matching Herb script protocol)
            String traderName = Players.getLocal().getName();
            Logger.log("MulingNode: Sending trader name: " + traderName);

            boolean messageSent = muleClient.sendMessage("TRADER_NAME:" + traderName);
            if (messageSent) {
                setStatus(MuleStatus.WAITING_FOR_MULE);
                return 100;
            } else {
                Logger.error("MulingNode: Failed to send trader name message");
                // Force disconnect and try again
                muleClient.disconnect();
                // Don't immediately return error - fall through to reconnect attempt
            }
        }

        // Attempt to connect with retries
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Add a delay before connection attempts (except first attempt)
                if (attempt > 1) {
                    int waitTime = 3000 * attempt;
                    Logger.log("MulingNode: Waiting " + (waitTime/1000) + " seconds before retry attempt " + attempt + "...");
                    Sleep.sleep(waitTime);
                }

                Logger.log("MulingNode: Connection attempt " + attempt + "/" + maxRetries + " to mule server...");

                // Check if the mule server is running before attempting to connect
                try (Socket testSocket = new Socket()) {
                    int port = config.getMulingConfig().getPort();
                    testSocket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 2000);
                    Logger.log("MulingNode: Mule server is reachable on port " + port);
                } catch (IOException e) {
                    Logger.error("MulingNode: Mule server is NOT reachable: " + e.getMessage());
                    Logger.log("MulingNode: Please make sure the Scar Mule Server script is running");

                    if (attempt == maxRetries) {
                        return handleError("Mule server is not running or not reachable");
                    }
                    continue;
                }

                // Now try to connect
                if (muleClient.connect()) {
                    Logger.log("MulingNode: Successfully connected to mule server");

                    // Wait a moment to ensure the connection is established
                    Sleep.sleep(1000);

                    // Send our username as the trader (matching Herb script protocol)
                    String traderName = Players.getLocal().getName();
                    Logger.log("MulingNode: Sending trader name: " + traderName);

                    boolean messageSent = muleClient.sendMessage("TRADER_NAME:" + traderName);
                    if (messageSent) {
                        setStatus(MuleStatus.WAITING_FOR_MULE);
                        return 100;
                    } else {
                        Logger.error("MulingNode: Failed to send trader name message");
                        if (attempt == maxRetries) {
                            return handleError("Failed to send message to mule");
                        }
                        // Try again
                        continue;
                    }
                } else {
                    Logger.error("MulingNode: Failed to connect to mule server (attempt " + attempt + "/" + maxRetries + ")");
                    Logger.error("MulingNode: Error: " + muleClient.getLastError());

                    if (attempt == maxRetries) {
                        return handleError("Failed to connect to mule: " + muleClient.getLastError());
                    }
                }
            } catch (Exception e) {
                Logger.error("MulingNode: Exception during connection: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging

                if (attempt == maxRetries) {
                    return handleError("Exception during mule connection: " + e.getMessage());
                }
            }
        }

        // If we get here, all retries failed
        return handleError("Failed to connect to mule server after " + maxRetries + " attempts");
    }

    private int handleWaitingForMule() {
        // Check for messages from the mule server
        String message = muleClient.readMessage();
        if (message != null) {
            Logger.log("MulingNode: Received message from mule server: " + message);

            // Handle specific messages
            if (message.contains("MULE_READY") || message.contains("READY_TO_TRADE") ||
                message.contains("TRADE_INIT") || message.contains("INITIATE_TRADE")) {
                Logger.log("MulingNode: Mule is ready to trade. Proceeding to trading state.");
                muleClient.sendMessage("READY_TO_TRADE");
                setStatus(MuleStatus.TRADING);
                return 100;
            }

            // Handle state reset messages
            if (message.contains("STATE_RESET")) {
                Logger.warn("MulingNode: Received STATE_RESET message from mule server: " + message);
                return handleError("Mule server reset its state: " + message);
            }
        }

        // Check if mule is nearby
        Player mulePlayer = Players.closest(p -> p != null && p.getName().equals(config.getMulingConfig().getMuleUsername()));
        if (mulePlayer == null) {
            Logger.log("Waiting for mule player (" + config.getMulingConfig().getMuleUsername() + ") to arrive...");
            // Check connection while waiting
            if (!muleClient.isConnected()) {
                Logger.warn("Lost connection to mule service while waiting for player.");
                return handleError("Lost connection to mule service.");
            }

            // Send status updates to the mule server
            muleClient.sendMessage("CLIENT_WAITING_AT_" + config.getMulingConfig().getMeetingSpot());
            muleClient.sendMessage("CLIENT_COORDS:" + Players.getLocal().getTile().getX() + "," +
                                  Players.getLocal().getTile().getY() + "," + Players.getLocal().getTile().getZ());

            // Send a periodic ping to keep the connection alive
            muleClient.sendMessage("PING");

            // Periodically check if we've been waiting too long and send a reset command
            // This helps recover from situations where the mule server is stuck
            long timeInState = System.currentTimeMillis() - lastStatusChangeTime;
            if (timeInState > 60000) { // If waiting for more than 1 minute
                Logger.warn("MulingNode: Been waiting for mule for " + (timeInState / 1000) + " seconds. Sending reset command.");
                muleClient.sendMessage("RESET:Client timeout - waiting too long for mule");
                // Don't immediately error out, give the server a chance to reset and respond
            }

            Sleep.sleep(1000, 1500);
            return 1000; // Return fixed sleep time
        }

        // Mule is nearby, send confirmation and proceed to trade
        Logger.log("Mule player found. Sending confirmation and initiating trade.");
        muleClient.sendMessage("MULE_FOUND");
        muleClient.sendMessage("READY_TO_TRADE");
        setStatus(MuleStatus.TRADING);
        return 100;
    }

    private int handleTrading() {
        // Reference trade users for cleaner code
        TradeUser us = TradeUser.US;
        TradeUser them = TradeUser.THEM;

        // First check if we're already in a trade
        if (!Trade.isOpen()) {
            // Find the mule player with exact name match and distance check
            Player mule = Players.closest(p -> p != null &&
                                       p.getName() != null &&
                                       p.getName().equals(config.getMulingConfig().getMuleUsername()) &&
                                       p.distance() <= 15);

            if (mule != null) {
                // Make sure we're close enough to trade
                if (mule.distance() > 2) {
                    Logger.log("Walking closer to mule for trade...");
                    Walking.walk(mule.getTile());
                    Sleep.sleepUntil(() -> mule.distance() <= 2, 5000);
                    return 600;
                }

                // Send a message to the mule server that we're initiating trade
                muleClient.sendMessage("INITIATING_TRADE");

                Logger.log("Attempting to trade with mule: " + mule.getName());
                if (mule.interact("Trade with")) {
                    // Wait for trade screen to open
                    if (Sleep.sleepUntil(() -> Trade.isOpen(), 5000)) {
                        Logger.log("Trade screen opened successfully");
                    } else {
                        Logger.warn("Trade screen did not open within timeout");
                        // Try again next loop
                        return 1000;
                    }
                } else {
                    Logger.warn("Failed to initiate trade with mule");
                    return 1000;
                }
            } else {
                Logger.warn("Mule player not found nearby or too far away");
                // Check if connection is still active
                if (!muleClient.isConnected()) {
                    Logger.error("Lost connection to mule server while trying to trade");
                    return handleError("Lost connection to mule server");
                }
                // Try to re-establish connection with mule
                return 2000;
            }
        }

        // At this point, we should be in a trade screen
        if (!Trade.isOpen()) {
            Logger.warn("Expected trade to be open but it's not!");
            return 1000;
        }

        // Check which trade screen we're on
        if (Trade.isOpen(1)) { // First trade screen
            // Get items to trade
            List<String> itemsToMuleNames = config.getMulingConfig().getItemsToMule();

            // Log what we're trying to mule
            Logger.log("Items to mule: " + String.join(", ", itemsToMuleNames));

            // Check if we've already offered all items
            boolean allItemsOffered = true;
            for (Item item : Inventory.all()) {
                if (item != null && itemsToMuleNames.contains(item.getName())) {
                    // Check if this item is already offered
                    Item offeredItem = Trade.getItem(true, item.getID());
                    int offeredAmount = (offeredItem != null) ? offeredItem.getAmount() : 0;

                    if (offeredAmount < item.getAmount()) {
                        allItemsOffered = false;
                        int amountToAdd = item.getAmount() - offeredAmount;

                        Logger.log("Offering item: " + item.getName() + " x" + amountToAdd);
                        if (Trade.addItem(item.getName(), amountToAdd)) {
                            Sleep.sleepUntil(() -> {
                                Item updated = Trade.getItem(true, item.getID());
                                return updated != null && updated.getAmount() >= item.getAmount();
                            }, 3000);

                            // Only add one item at a time to avoid issues
                            break;
                        } else {
                            Logger.warn("Failed to offer item: " + item.getName());
                        }
                    }
                }
            }

            // If all items are offered, check if we've accepted
            if (allItemsOffered) {
                if (!Trade.hasAcceptedTrade(us)) {
                    Logger.log("All items offered, accepting first trade screen");
                    if (Trade.acceptTrade(1)) {
                        Sleep.sleepUntil(() -> Trade.hasAcceptedTrade(us) || Trade.isOpen(2), 5000);
                        return 600;
                    }
                } else {
                    Logger.log("Waiting for mule to accept first screen");
                    // Send a message to the mule server that we're waiting
                    muleClient.sendMessage("WAITING_FOR_MULE_ACCEPT_1");
                }
            }
        } else if (Trade.isOpen(2)) { // Second trade screen
            if (!Trade.hasAcceptedTrade(us)) {
                Logger.log("Accepting second trade screen");
                if (Trade.acceptTrade(2)) {
                    Sleep.sleepUntil(() -> Trade.hasAcceptedTrade(us) || !Trade.isOpen(), 5000);
                    if (!Trade.isOpen()) {
                        setStatus(MuleStatus.WAITING_FOR_COMPLETION);
                    }
                    return 600;
                }
            } else {
                Logger.log("Waiting for mule to accept second screen");
                // Send a message to the mule server that we're waiting
                muleClient.sendMessage("WAITING_FOR_MULE_ACCEPT_2");

                if (Trade.hasAcceptedTrade(them)) {
                    Logger.log("Both accepted second screen");
                    setStatus(MuleStatus.WAITING_FOR_COMPLETION);
                }
            }
        }

        return 600;
    }

    private int handleWaitingForMuleConfirm() {
        // This state is now handled directly in the handleTrading method
        // Just transition back to TRADING state to let it handle everything
        setStatus(MuleStatus.TRADING);
        return 100;
    }

    private int handleWaitForCompletion() {
        // Check if trade is still open
        if (Trade.isOpen()) {
            Logger.log("Waiting for trade to complete...");
            // Send a message to the mule server that we're waiting for completion
            muleClient.sendMessage("WAITING_FOR_TRADE_COMPLETION");
            return 1000;
        } else {
            // Trade completed or was declined
            // Check if we still have the items we were trying to mule
            List<String> itemsToMuleNames = config.getMulingConfig().getItemsToMule();
            boolean stillHasItems = false;

            // Check each item individually and log it
            for (String itemName : itemsToMuleNames) {
                int count = Inventory.count(itemName);
                Logger.log("Post-trade inventory check: " + itemName + " x" + count);
                if (count > 0) {
                    stillHasItems = true;
                }
            }

            if (stillHasItems) {
                Logger.warn("Trade appears to have been declined or canceled");
                // Send a message to the mule server about the failed trade
                muleClient.sendMessage("TRADE_FAILED");
                setStatus(MuleStatus.TRADING); // Try to trade again
                return 1000;
            } else {
                Logger.log("Trade completed successfully!");
                // Send a message to the mule server about the successful trade
                muleClient.sendMessage("TRADE_COMPLETE");
                setStatus(MuleStatus.COMPLETE);
                return 100;
            }
        }
    }

    private int handleComplete() {
        Logger.log("Muling process completed successfully");
        // We already sent TRADE_COMPLETE in handleWaitForCompletion
        // Just proceed to disconnecting
        setStatus(MuleStatus.DISCONNECTING);
        return 100;
    }

    private int handleDisconnecting() {
        // Clean up connection
        cleanupMuleConnection();
        Logger.log("Muling process finished, returning to normal script operation");
        setStatus(MuleStatus.IDLE);
        return 1000;
    }

    private int handleError(String errorMessage) {
        Logger.error("MulingNode ERROR: " + errorMessage);

        // Send error and reset command to mule server if connected
        if (muleClient != null && muleClient.isConnected()) {
            Logger.log("MulingNode: Sending error message to mule server");
            muleClient.sendMessage("ERROR:" + errorMessage);

            // Also send a reset command to force the mule server to reset its state
            Logger.log("MulingNode: Sending RESET command to mule server");
            muleClient.sendMessage("RESET:Client error - " + errorMessage);

            // Give the server a moment to process the reset
            Sleep.sleep(1000);
        }

        // Clean up connection
        cleanupMuleConnection();

        // Reset status to IDLE after a delay
        setStatus(MuleStatus.ERROR);
        Logger.log("MulingNode: In ERROR state, waiting 5 seconds before resetting to IDLE");
        Sleep.sleep(5000); // Wait 5 seconds in error state
        setStatus(MuleStatus.IDLE);
        return 5000;
    }

    private void cleanupMuleConnection() {
        if (muleClient != null) {
            muleClient.disconnect();
        }
    }

    /**
     * Verifies that the mule settings are properly configured
     * @return true if settings appear valid, false otherwise
     */
    private boolean verifyMuleSettings() {
        Logger.info("===============================");
        Logger.info("VERIFYING MULE SETTINGS");
        Logger.info("===============================");

        boolean allValid = true;

        // Get the framework-level muling configuration
        core.config.MulingConfig mulingConfig = config.getMulingConfig();

        // Check port
        int port = mulingConfig.getPort();
        if (port <= 0 || port > 65535) {
            Logger.error("INVALID PORT: " + port + " (must be between 1-65535)");
            allValid = false;
        } else {
            Logger.info("Port setting: " + port + " ✓");
        }

        // Check mule username
        String muleUsername = mulingConfig.getMuleUsername();
        if (muleUsername == null || muleUsername.trim().isEmpty()) {
            Logger.error("INVALID MULE USERNAME: Username not specified");
            allValid = false;
        } else {
            Logger.info("Mule username: " + muleUsername + " ✓");
        }

        // Check world
        int world = mulingConfig.getMuleWorld();
        if (world <= 0) {
            Logger.error("INVALID WORLD: " + world);
            allValid = false;
        } else {
            Logger.info("World setting: " + world + " ✓");
        }

        // Check items to mule
        List<String> itemsToMule = mulingConfig.getItemsToMule();
        if (itemsToMule == null || itemsToMule.isEmpty()) {
            Logger.error("NO ITEMS CONFIGURED FOR MULING");
            allValid = false;
        } else {
            Logger.info("Items to mule: " + String.join(", ", itemsToMule) + " ✓");
        }

        // Check if mule server is running on specified port
        try (Socket testSocket = new Socket()) {
            testSocket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 3000);
            Logger.info("Mule server is running and listening on port " + port + " ✓");
        } catch (IOException e) {
            Logger.error("MULE SERVER NOT RUNNING ON PORT " + port);
            Logger.error("Make sure the mule script is running and correctly configured");
            allValid = false;
        }

        Logger.info("===============================");
        if (allValid) {
            Logger.info("Mule settings verification PASSED ✓");
        } else {
            Logger.error("Mule settings verification FAILED ✗");
        }
        Logger.info("===============================");

        return allValid;
    }

    /**
     * Calculate the value of Wrath runes in the inventory
     * @return The count of Wrath runes
     */
    private int calculateWrathRuneCount() {
        return Inventory.count(WRATH_RUNE);
    }

    public void onMessage(Message message) {
        // Handle trade-related messages
        if (message.getMessage().contains("wishes to trade with you")) {
            String playerName = message.getMessage().split(" wishes")[0];
            if (playerName.equals(config.getMulingConfig().getMuleUsername())) {
                Logger.log("Received trade request from mule: " + playerName);
            }
        }
    }
}
