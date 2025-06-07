package core.config;

// Commented out GSON imports for now to allow compilation without GSON dependency
// import com.google.gson.JsonObject;
// import com.google.gson.annotations.Expose;
// import org.dreambot.api.utilities.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified framework-level restock configuration using multiplier approach
 * Users set usage per trip and multiplier - system calculates min/max automatically
 */
public class RestockConfig {
    
    /**
     * Represents a single item's restock configuration
     */
    public static class RestockItem {
        // @Expose 
        public String itemName;
        // @Expose 
        public int usagePerTrip;      // How many used per trip (minimum threshold)
        // @Expose 
        public double multiplier;     // Multiplier for maximum stock (default 6.0)
        // @Expose 
        public boolean enabled;       // Whether restocking is enabled for this item
        // @Expose 
        public boolean sellExcess;    // Whether to sell excess items
        // @Expose 
        public int sellThreshold;     // Sell when above this amount (calculated automatically)
        // @Expose 
        public String category;       // Item category (Food, Potion, Gear, etc.)
        
        public RestockItem() {}
        
        public RestockItem(String itemName, int usagePerTrip) {
            this.itemName = itemName;
            this.usagePerTrip = usagePerTrip;
            this.multiplier = 6.0; // Default to 6x multiplier
            this.enabled = true;
            this.sellExcess = false;
            this.sellThreshold = getMaxStock() + (usagePerTrip * 2); // Sell when 2 trips above max
            this.category = null; // Will be determined later
        }
        
        public RestockItem(String itemName, int usagePerTrip, double multiplier) {
            this.itemName = itemName;
            this.usagePerTrip = usagePerTrip;
            this.multiplier = multiplier;
            this.enabled = true;
            this.sellExcess = false;
            this.sellThreshold = getMaxStock() + (usagePerTrip * 2);
            this.category = null; // Will be determined later
        }
        
        public RestockItem(String itemName, int usagePerTrip, double multiplier, String category) {
            this.itemName = itemName;
            this.usagePerTrip = usagePerTrip;
            this.multiplier = multiplier;
            this.enabled = true;
            this.sellExcess = false;
            this.sellThreshold = getMaxStock() + (usagePerTrip * 2);
            this.category = category;
        }
        
        /**
         * Gets the minimum stock (triggers restock when below this)
         */
        public int getMinStock() {
            return usagePerTrip;
        }
        
        /**
         * Gets the maximum stock (restock to this amount)
         */
        public int getMaxStock() {
            return (int) (usagePerTrip * multiplier);
        }
        
        /**
         * Updates sell threshold based on current settings
         */
        public void updateSellThreshold() {
            this.sellThreshold = getMaxStock() + (usagePerTrip * 2);
        }
    }
    
    // Main configuration
    // @Expose 
    private boolean enabled = true;
    // @Expose 
    private double defaultMultiplier = 6.0;
    // @Expose 
    private Map<String, RestockItem> restockItems = new HashMap<>();
    // @Expose 
    private Map<String, Integer> currentQuantities = new HashMap<>(); // Cached quantities
    
    /**
     * Default constructor
     */
    public RestockConfig() {
        this.enabled = true;
        this.defaultMultiplier = 6.0;
        this.restockItems = new HashMap<>();
        this.currentQuantities = new HashMap<>();
    }
    
    /**
     * Copy constructor
     */
    public RestockConfig(RestockConfig other) {
        if (other != null) {
            this.enabled = other.enabled;
            this.defaultMultiplier = other.defaultMultiplier;
            this.restockItems = new HashMap<>();
            for (Map.Entry<String, RestockItem> entry : other.restockItems.entrySet()) {
                RestockItem item = entry.getValue();
                this.restockItems.put(entry.getKey(), new RestockItem(item.itemName, item.usagePerTrip, item.multiplier, item.category));
            }
            this.currentQuantities = new HashMap<>(other.currentQuantities);
        }
    }
    
    // Basic getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public double getDefaultMultiplier() { return defaultMultiplier; }
    public void setDefaultMultiplier(double defaultMultiplier) { this.defaultMultiplier = defaultMultiplier; }
    
    /**
     * Adds or updates a restock item
     */
    public void addItem(String itemName, int usagePerTrip) {
        restockItems.put(itemName, new RestockItem(itemName, usagePerTrip, defaultMultiplier));
    }
    
    public void addItem(String itemName, int usagePerTrip, double multiplier) {
        restockItems.put(itemName, new RestockItem(itemName, usagePerTrip, multiplier));
    }
    
    /**
     * Removes a restock item
     */
    public void removeItem(String itemName) {
        restockItems.remove(itemName);
        currentQuantities.remove(itemName);
    }
    
    /**
     * Gets a restock item
     */
    public RestockItem getItem(String itemName) {
        return restockItems.get(itemName);
    }
    
    /**
     * Gets all restock items
     */
    public Map<String, RestockItem> getAllItems() {
        return restockItems;
    }
    
    /**
     * Gets list of item names to restock
     */
    public List<String> getItemsToRestock() {
        List<String> items = new ArrayList<>();
        for (RestockItem item : restockItems.values()) {
            if (item.enabled) {
                items.add(item.itemName);
            }
        }
        return items;
    }
    
    /**
     * Checks if an item exists in restock config
     */
    public boolean containsItem(String itemName) {
        return restockItems.containsKey(itemName);
    }
    
    /**
     * Updates multiplier for all items
     */
    public void updateAllMultipliers(double newMultiplier) {
        this.defaultMultiplier = newMultiplier;
        for (RestockItem item : restockItems.values()) {
            item.multiplier = newMultiplier;
            item.updateSellThreshold();
        }
    }
    
    // Current quantity tracking
    public void setCurrentQuantity(String itemName, int quantity) {
        currentQuantities.put(itemName, quantity);
    }
    
    public int getCurrentQuantity(String itemName) {
        return currentQuantities.getOrDefault(itemName, 0);
    }
    
    public Map<String, Integer> getCurrentQuantities() {
        return currentQuantities;
    }
    
    // Legacy compatibility methods (for existing code)
    public List<String> getInventoryItemsToRestock() {
        return getItemsToRestock();
    }
    
    public int getInventoryItemMinimumQuantity(String itemName) {
        RestockItem item = restockItems.get(itemName);
        return item != null ? item.getMinStock() : 0;
    }
    
    public int getInventoryItemDesiredQuantity(String itemName) {
        RestockItem item = restockItems.get(itemName);
        return item != null ? item.getMaxStock() : 0;
    }
    
    public void setInventoryItemMinimumQuantity(String itemName, int quantity) {
        RestockItem item = restockItems.get(itemName);
        if (item != null) {
            item.usagePerTrip = quantity;
        }
    }
    
    public void setInventoryItemDesiredQuantity(String itemName, int quantity) {
        RestockItem item = restockItems.get(itemName);
        if (item != null) {
            // Calculate multiplier from desired quantity
            item.multiplier = (double) quantity / item.usagePerTrip;
            item.updateSellThreshold();
        }
    }
    
    public boolean isSelling(String itemName) {
        RestockItem item = restockItems.get(itemName);
        return item != null && item.sellExcess;
    }
    
    public int getSellQuantity(String itemName) {
        RestockItem item = restockItems.get(itemName);
        return item != null ? item.sellThreshold : 0;
    }
    
    public void setIsSelling(String itemName, boolean selling) {
        RestockItem item = restockItems.get(itemName);
        if (item != null) {
            item.sellExcess = selling;
        }
    }
    
    public void setSellQuantity(String itemName, int quantity) {
        RestockItem item = restockItems.get(itemName);
        if (item != null) {
            item.sellThreshold = quantity;
        }
    }
    
    public Map<String, Boolean> getIsSelling() {
        Map<String, Boolean> selling = new HashMap<>();
        for (RestockItem item : restockItems.values()) {
            selling.put(item.itemName, item.sellExcess);
        }
        return selling;
    }
    
    public Map<String, Integer> getSellQuantities() {
        Map<String, Integer> quantities = new HashMap<>();
        for (RestockItem item : restockItems.values()) {
            quantities.put(item.itemName, item.sellThreshold);
        }
        return quantities;
    }
    
    // Legacy methods for compatibility
    public Map<String, Integer> getInventoryItemMinimumQuantities() {
        Map<String, Integer> quantities = new HashMap<>();
        for (RestockItem item : restockItems.values()) {
            quantities.put(item.itemName, item.getMinStock());
        }
        return quantities;
    }
    
    public Map<String, Integer> getInventoryItemDesiredQuantities() {
        Map<String, Integer> quantities = new HashMap<>();
        for (RestockItem item : restockItems.values()) {
            quantities.put(item.itemName, item.getMaxStock());
        }
        return quantities;
    }
    
    public void addInventoryItem(String itemName) {
        addItem(itemName, 1);
    }
    
    public void addInventoryItem(String itemName, int desiredQuantity) {
        addItem(itemName, Math.max(1, desiredQuantity / (int)defaultMultiplier), defaultMultiplier);
    }
    
    public void removeInventoryItem(String itemName) {
        removeItem(itemName);
    }
    
    public boolean containsInventoryItem(String itemName) {
        return containsItem(itemName);
    }
    
    public int getDefaultMinimumQuantity() {
        return 1; // Not used in new system
    }
    
    public void setDefaultMinimumQuantity(int quantity) {
        // Not used in new system
    }
    
    // TODO: Re-enable JSON serialization when GSON is available in classpath
    /*
    /**
     * JSON serialization
     */
    /*
    public JsonObject toJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("defaultMultiplier", defaultMultiplier);
        
        JsonObject itemsJson = new JsonObject();
        for (Map.Entry<String, RestockItem> entry : restockItems.entrySet()) {
            JsonObject itemJson = new JsonObject();
            RestockItem item = entry.getValue();
            itemJson.addProperty("itemName", item.itemName);
            itemJson.addProperty("usagePerTrip", item.usagePerTrip);
            itemJson.addProperty("multiplier", item.multiplier);
            itemJson.addProperty("enabled", item.enabled);
            itemJson.addProperty("sellExcess", item.sellExcess);
            itemJson.addProperty("sellThreshold", item.sellThreshold);
            itemsJson.add(entry.getKey(), itemJson);
        }
        json.add("restockItems", itemsJson);
        
        JsonObject currentQtyJson = new JsonObject();
        for (Map.Entry<String, Integer> entry : currentQuantities.entrySet()) {
            currentQtyJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("currentQuantities", currentQtyJson);
        
        return json;
    }
    */
    
    // TODO: Re-enable JSON deserialization when GSON is available in classpath
    /*
    /**
     * JSON deserialization
     */
    /*
    public void fromJSON(JsonObject json) {
        if (json.has("enabled")) {
            enabled = json.get("enabled").getAsBoolean();
        }
        if (json.has("defaultMultiplier")) {
            defaultMultiplier = json.get("defaultMultiplier").getAsDouble();
        }
        
        if (json.has("restockItems")) {
            JsonObject itemsJson = json.getAsJsonObject("restockItems");
            restockItems.clear();
            for (String key : itemsJson.keySet()) {
                JsonObject itemJson = itemsJson.getAsJsonObject(key);
                RestockItem item = new RestockItem();
                item.itemName = itemJson.get("itemName").getAsString();
                item.usagePerTrip = itemJson.get("usagePerTrip").getAsInt();
                item.multiplier = itemJson.has("multiplier") ? itemJson.get("multiplier").getAsDouble() : defaultMultiplier;
                item.enabled = itemJson.has("enabled") ? itemJson.get("enabled").getAsBoolean() : true;
                item.sellExcess = itemJson.has("sellExcess") ? itemJson.get("sellExcess").getAsBoolean() : false;
                item.sellThreshold = itemJson.has("sellThreshold") ? itemJson.get("sellThreshold").getAsInt() : item.getMaxStock() + (item.usagePerTrip * 2);
                restockItems.put(key, item);
            }
        }
        
        if (json.has("currentQuantities")) {
            JsonObject currentQtyJson = json.getAsJsonObject("currentQuantities");
            currentQuantities.clear();
            for (String key : currentQtyJson.keySet()) {
                currentQuantities.put(key, currentQtyJson.get(key).getAsInt());
            }
        }
    }
    */
    
    /**
     * Logs the configuration
     */
    public void logConfig() {
        System.out.println("=== SIMPLIFIED RESTOCK CONFIGURATION ===");
        System.out.println("Enabled: " + enabled);
        System.out.println("Default Multiplier: " + defaultMultiplier + "x");
        System.out.println("Items configured: " + restockItems.size());
        
        for (RestockItem item : restockItems.values()) {
            System.out.printf("Item: %s - Usage/Trip: %d - Multiplier: %.1fx - Min: %d - Max: %d - Enabled: %s%n", 
                item.itemName, item.usagePerTrip, item.multiplier, item.getMinStock(), item.getMaxStock(), item.enabled);
            if (item.sellExcess) {
                System.out.println("  → Sell excess above: " + item.sellThreshold);
            }
        }
        System.out.println("=======================================");
    }
    
    /**
     * Merges another config into this one
     */
    public void merge(RestockConfig other) {
        if (other == null || !other.isEnabled()) return;
        
        this.enabled = other.enabled;
        this.defaultMultiplier = other.defaultMultiplier;
        
        // Merge items
        this.restockItems.clear();
        for (Map.Entry<String, RestockItem> entry : other.restockItems.entrySet()) {
            RestockItem item = entry.getValue();
            this.restockItems.put(entry.getKey(), new RestockItem(item.itemName, item.usagePerTrip, item.multiplier));
        }
    }
}
