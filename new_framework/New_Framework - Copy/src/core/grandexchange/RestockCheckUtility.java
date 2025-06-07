package core.grandexchange;

import core.config.ScriptConfig;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.utilities.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

/**
 * Utility class for checking restock needs during banking activities
 */
public class RestockCheckUtility {
    private final ScriptConfig config;

    public RestockCheckUtility(ScriptConfig config) {
        this.config = config;
    }

    /**
     * Checks if any items need restocking
     * @return true if restock is needed, false otherwise
     */
    public boolean needsRestock() {

        // Check if restock is enabled
        if (!config.getRestockConfig().isEnabled()) {
            Logger.info("[RestockCheck] Restock is disabled in config");
            return false;
        }

        boolean needsRestock = false;
        List<String> itemsToCheck = new ArrayList<>();
        
        Logger.info("[RestockCheck] == Starting restock check ==");
        
        // Add all items that could potentially need restocking
        List<String> inventoryItems = config.getRestockConfig().getInventoryItemsToRestock();
        Map<String, Boolean> sellingItems = config.getRestockConfig().getIsSelling();
        
        Logger.info("[RestockCheck] Inventory items to check: " + 
                   (inventoryItems.isEmpty() ? "none" : String.join(", ", inventoryItems)));
        Logger.info("[RestockCheck] Items configured for selling: " + 
                   (sellingItems.isEmpty() ? "none" : String.join(", ", sellingItems.keySet())));
        
        itemsToCheck.addAll(inventoryItems);
        itemsToCheck.addAll(sellingItems.keySet());

        // Remove duplicates
        itemsToCheck = new ArrayList<>(new LinkedHashSet<>(itemsToCheck));
        
        Logger.info("[RestockCheck] Total unique items to check: " + itemsToCheck.size());
        
        // First update all current quantities from bank and inventory
        for (String item : itemsToCheck) {
            int bankQuantity = Bank.count(item);
            int inventoryQuantity = Inventory.count(item);
            int totalQuantity = bankQuantity + inventoryQuantity;
            
            config.getRestockConfig().setCurrentQuantity(item, totalQuantity);
            Logger.info("[RestockCheck] Updated " + item + " current quantity to " + totalQuantity + 
                       " (Bank: " + bankQuantity + ", Inventory: " + inventoryQuantity + ")");
        }

        // Now check each item against thresholds
        for (String item : itemsToCheck) {
            int totalQuantity = config.getRestockConfig().getCurrentQuantity(item);
            
            boolean isSelling = config.getRestockConfig().isSelling(item);
            int sellQuantity = config.getRestockConfig().getSellQuantity(item);
            
            if (isSelling) {
                Logger.info("[RestockCheck] Sell item: " + item + 
                           " - Total quantity: " + totalQuantity + 
                           " - Sell threshold: " + sellQuantity);
                           
                if (totalQuantity >= sellQuantity && sellQuantity > 0) {
                    Logger.info("[RestockCheck] ** Need to sell " + item + 
                               " (have " + totalQuantity + ", threshold " + sellQuantity + ")");
                    needsRestock = true;
                }
            } else {
                int minimumQuantity = config.getRestockConfig().getInventoryItemMinimumQuantity(item);
                
                Logger.info("[RestockCheck] Buy item: " + item + 
                           " - Total quantity: " + totalQuantity + 
                           " - Minimum threshold: " + minimumQuantity);
                           
                if (totalQuantity < minimumQuantity) {
                    Logger.info("[RestockCheck] ** Need to buy " + item + 
                               " (have " + totalQuantity + ", need " + minimumQuantity + ")");
                    needsRestock = true;
                }
            }
        }

        Logger.info("[RestockCheck] Final result: " + (needsRestock ? "NEED RESTOCK" : "NO RESTOCK NEEDED"));
        return needsRestock;
    }

    /**
     * Gets a list of items that need to be sold
     * @return List of items that need to be sold
     */
    public List<String> getItemsToSell() {
        List<String> itemsToSell = new ArrayList<>();
        
        for (Map.Entry<String, Boolean> entry : config.getRestockConfig().getIsSelling().entrySet()) {
            String item = entry.getKey();
            boolean isSelling = entry.getValue();
            
            if (isSelling) {
                int bankQuantity = Bank.count(item);
                int inventoryQuantity = Inventory.count(item);
                int totalQuantity = bankQuantity + inventoryQuantity;
                int sellQuantity = config.getRestockConfig().getSellQuantity(item);
                
                Logger.info("[RestockCheck] Checking sell item: " + item + 
                           " - Is Selling: " + isSelling +
                           " - Total Quantity: " + totalQuantity + 
                           " (Bank: " + bankQuantity + ", Inventory: " + inventoryQuantity + ")" +
                           " - Sell Threshold: " + sellQuantity);
                           
                if (sellQuantity > 0 && totalQuantity >= sellQuantity) {
                    Logger.info("[RestockCheck] Adding " + item + " to sell list");
                    itemsToSell.add(item);
                } else {
                    Logger.info("[RestockCheck] Not adding " + item + " to sell list - quantity threshold not met");
                }
            }
        }
        
        if (itemsToSell.isEmpty()) {
            Logger.info("[RestockCheck] No items to sell found");
        } else {
            Logger.info("[RestockCheck] Items to sell: " + String.join(", ", itemsToSell));
        }
        
        return itemsToSell;
    }

    /**
     * Gets a map of items that need to be bought and the quantity needed to reach desired quantity
     * @return Map of item name to quantity to buy
     */
    public Map<String, Integer> getItemsToBuy() {
        Map<String, Integer> itemsToBuy = new java.util.HashMap<>();
        Logger.info("[RestockCheck] Starting check for items to buy");
        Logger.info("[RestockCheck] Items configured for restocking: " + 
                   String.join(", ", config.getRestockConfig().getInventoryItemsToRestock()));
        Logger.info("[RestockCheck] Custom minimum quantities configured: " + 
                   config.getRestockConfig().getInventoryItemMinimumQuantities());
        Logger.info("[RestockCheck] Desired quantities configured: " + 
                   config.getRestockConfig().getInventoryItemDesiredQuantities());
        boolean shouldRestock = false;
        Map<String, Integer> totalQuantities = new java.util.HashMap<>();
        // First pass: check if any item is below minimum
        for (String item : config.getRestockConfig().getInventoryItemsToRestock()) {
            if (config.getRestockConfig().isSelling(item)) continue;
            int bankQuantity = Bank.count(item);
            int inventoryQuantity = Inventory.count(item);
            int totalQuantity = bankQuantity + inventoryQuantity;
            int minimumQuantity = config.getRestockConfig().getInventoryItemMinimumQuantity(item);
            
            totalQuantities.put(item, totalQuantity);
            Logger.info("[RestockCheck] Item: " + item + 
                       " - Total quantity: " + totalQuantity +
                       " (Bank: " + bankQuantity + ", Inventory: " + inventoryQuantity + ")" +
                       " - Minimum threshold: " + minimumQuantity);
                       
            if (totalQuantity < minimumQuantity) {
                shouldRestock = true;
            }
        }
        // If any item is below minimum, restock all items to desired quantity
        if (shouldRestock) {
            for (String item : config.getRestockConfig().getInventoryItemsToRestock()) {
                if (config.getRestockConfig().isSelling(item)) continue;
                int totalQuantity = totalQuantities.get(item);
                int desiredQuantity = config.getRestockConfig().getInventoryItemDesiredQuantity(item);
                if (desiredQuantity > totalQuantity) {
                    int amountToBuy = desiredQuantity - totalQuantity;
                    Logger.info("[RestockCheck] Adding " + item + " to buy list with quantity " + amountToBuy);
                    itemsToBuy.put(item, amountToBuy);
                }
            }
        }
        if (itemsToBuy.isEmpty()) {
            Logger.info("[RestockCheck] No items to buy found");
        } else {
            Logger.info("[RestockCheck] Items to buy: " + String.join(", ", itemsToBuy.keySet()));
            for (String item : itemsToBuy.keySet()) {
                Logger.info("[RestockCheck] Will buy " + itemsToBuy.get(item) + "x " + item);
            }
        }
        return itemsToBuy;
    }

    /**
     * Logs detailed information about the restock configuration
     */
    public void logRestockConfig() {
        Logger.info("=== RESTOCK CONFIGURATION ===");
        Logger.info("Restock Enabled: " + config.getRestockConfig().isEnabled());
        Logger.info("Default Minimum Quantity: " + config.getRestockConfig().getDefaultMinimumQuantity());
        Logger.info("--- ITEMS TO BUY ---");
        List<String> inventoryItems = config.getRestockConfig().getInventoryItemsToRestock();
        if (inventoryItems.isEmpty()) {
            Logger.info("No inventory items configured for restocking");
        } else {
            for (String item : inventoryItems) {
                int bankQty = Bank.count(item);
                int invQty = Inventory.count(item);
                int totalQty = bankQty + invQty;
                int minQty = config.getRestockConfig().getInventoryItemMinimumQuantity(item);
                int desiredQty = config.getRestockConfig().getInventoryItemDesiredQuantity(item);
                Logger.info(String.format("Item: %s - Current: %d (Bank: %d, Inv: %d) - Min: %d - Desired: %d", 
                                         item, totalQty, bankQty, invQty, minQty, desiredQty));
            }
        }
        
        Logger.info("--- ITEMS TO SELL ---");
        Map<String, Boolean> sellingItems = config.getRestockConfig().getIsSelling();
        if (sellingItems.isEmpty()) {
            Logger.info("No items configured for selling");
        } else {
            for (Map.Entry<String, Boolean> entry : sellingItems.entrySet()) {
                if (entry.getValue()) {
                    String item = entry.getKey();
                    int bankQty = Bank.count(item);
                    int invQty = Inventory.count(item);
                    int totalQty = bankQty + invQty;
                    int sellQty = config.getRestockConfig().getSellQuantity(item);
                    Logger.info(String.format("Item: %s - Current: %d (Bank: %d, Inv: %d) - Sell Threshold: %d", 
                                             item, totalQty, bankQty, invQty, sellQty));
                }
            }
        }
        
        Logger.info("============================");
    }
} 
