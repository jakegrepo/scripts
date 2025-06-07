package core.utils;

import java.util.*;

/**
 * Utility class for categorizing and providing intelligent suggestions for restock items
 */
public class RestockItemUtils {
    
    // Categories for intelligent usage estimation
    private static final Map<String, Integer> FOOD_USAGE_ESTIMATES = new HashMap<>();
    private static final Map<String, Integer> POTION_USAGE_ESTIMATES = new HashMap<>();
    private static final Map<String, Integer> RUNE_USAGE_ESTIMATES = new HashMap<>();
    private static final Map<String, Integer> AMMO_USAGE_ESTIMATES = new HashMap<>();
    private static final Map<String, Integer> TELEPORT_USAGE_ESTIMATES = new HashMap<>();
    private static final Set<String> EXCLUDED_ITEMS = new HashSet<>();
    
    static {
        // Food items - estimated usage per trip
        FOOD_USAGE_ESTIMATES.put("Shark", 15);
        FOOD_USAGE_ESTIMATES.put("Karambwan", 20);
        FOOD_USAGE_ESTIMATES.put("Manta ray", 12);
        FOOD_USAGE_ESTIMATES.put("Dark crab", 10);
        FOOD_USAGE_ESTIMATES.put("Anglerfish", 8);
        FOOD_USAGE_ESTIMATES.put("Cooked karambwan", 20);
        FOOD_USAGE_ESTIMATES.put("Lobster", 20);
        FOOD_USAGE_ESTIMATES.put("Monkfish", 18);
        FOOD_USAGE_ESTIMATES.put("Swordfish", 22);
        FOOD_USAGE_ESTIMATES.put("Tuna", 25);
        FOOD_USAGE_ESTIMATES.put("Salmon", 30);
        
        // Combat potions
        POTION_USAGE_ESTIMATES.put("Super combat potion(4)", 1);
        POTION_USAGE_ESTIMATES.put("Super combat potion(3)", 1);
        POTION_USAGE_ESTIMATES.put("Super combat potion(2)", 1);
        POTION_USAGE_ESTIMATES.put("Super combat potion(1)", 1);
        POTION_USAGE_ESTIMATES.put("Combat potion(4)", 1);
        POTION_USAGE_ESTIMATES.put("Combat potion(3)", 1);
        POTION_USAGE_ESTIMATES.put("Combat potion(2)", 1);
        POTION_USAGE_ESTIMATES.put("Combat potion(1)", 1);
        POTION_USAGE_ESTIMATES.put("Super strength(4)", 1);
        POTION_USAGE_ESTIMATES.put("Super strength(3)", 1);
        POTION_USAGE_ESTIMATES.put("Super strength(2)", 1);
        POTION_USAGE_ESTIMATES.put("Super strength(1)", 1);
        POTION_USAGE_ESTIMATES.put("Super attack(4)", 1);
        POTION_USAGE_ESTIMATES.put("Super attack(3)", 1);
        POTION_USAGE_ESTIMATES.put("Super attack(2)", 1);
        POTION_USAGE_ESTIMATES.put("Super attack(1)", 1);
        POTION_USAGE_ESTIMATES.put("Super defence(4)", 1);
        POTION_USAGE_ESTIMATES.put("Super defence(3)", 1);
        POTION_USAGE_ESTIMATES.put("Super defence(2)", 1);
        POTION_USAGE_ESTIMATES.put("Super defence(1)", 1);
        POTION_USAGE_ESTIMATES.put("Ranging potion(4)", 1);
        POTION_USAGE_ESTIMATES.put("Ranging potion(3)", 1);
        POTION_USAGE_ESTIMATES.put("Ranging potion(2)", 1);
        POTION_USAGE_ESTIMATES.put("Ranging potion(1)", 1);
        POTION_USAGE_ESTIMATES.put("Prayer potion(4)", 2);
        POTION_USAGE_ESTIMATES.put("Prayer potion(3)", 2);
        POTION_USAGE_ESTIMATES.put("Prayer potion(2)", 2);
        POTION_USAGE_ESTIMATES.put("Prayer potion(1)", 2);
        POTION_USAGE_ESTIMATES.put("Super restore(4)", 2);
        POTION_USAGE_ESTIMATES.put("Super restore(3)", 2);
        POTION_USAGE_ESTIMATES.put("Super restore(2)", 2);
        POTION_USAGE_ESTIMATES.put("Super restore(1)", 2);
        
        // Runes for common spells
        RUNE_USAGE_ESTIMATES.put("Air rune", 100);
        RUNE_USAGE_ESTIMATES.put("Water rune", 100);
        RUNE_USAGE_ESTIMATES.put("Earth rune", 100);
        RUNE_USAGE_ESTIMATES.put("Fire rune", 100);
        RUNE_USAGE_ESTIMATES.put("Mind rune", 50);
        RUNE_USAGE_ESTIMATES.put("Chaos rune", 50);
        RUNE_USAGE_ESTIMATES.put("Death rune", 30);
        RUNE_USAGE_ESTIMATES.put("Blood rune", 20);
        RUNE_USAGE_ESTIMATES.put("Soul rune", 20);
        RUNE_USAGE_ESTIMATES.put("Nature rune", 50);
        RUNE_USAGE_ESTIMATES.put("Law rune", 20);
        RUNE_USAGE_ESTIMATES.put("Cosmic rune", 30);
        RUNE_USAGE_ESTIMATES.put("Astral rune", 30);
        RUNE_USAGE_ESTIMATES.put("Wrath rune", 10);
        
        // Ammunition
        AMMO_USAGE_ESTIMATES.put("Dragon arrow", 200);
        AMMO_USAGE_ESTIMATES.put("Rune arrow", 250);
        AMMO_USAGE_ESTIMATES.put("Adamant arrow", 300);
        AMMO_USAGE_ESTIMATES.put("Mithril arrow", 350);
        AMMO_USAGE_ESTIMATES.put("Steel arrow", 400);
        AMMO_USAGE_ESTIMATES.put("Iron arrow", 450);
        AMMO_USAGE_ESTIMATES.put("Bronze arrow", 500);
        AMMO_USAGE_ESTIMATES.put("Dragon dart", 150);
        AMMO_USAGE_ESTIMATES.put("Rune dart", 200);
        AMMO_USAGE_ESTIMATES.put("Adamant dart", 250);
        AMMO_USAGE_ESTIMATES.put("Dragon bolt", 100);
        AMMO_USAGE_ESTIMATES.put("Runite bolt", 120);
        AMMO_USAGE_ESTIMATES.put("Dragon bolt (e)", 80);
        AMMO_USAGE_ESTIMATES.put("Runite bolt (e)", 100);
        
        // Teleports
        TELEPORT_USAGE_ESTIMATES.put("Teleport to house", 5);
        TELEPORT_USAGE_ESTIMATES.put("Varrock teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Lumbridge teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Falador teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Camelot teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Ardougne teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Watchtower teleport", 3);
        TELEPORT_USAGE_ESTIMATES.put("Games necklace(8)", 1);
        TELEPORT_USAGE_ESTIMATES.put("Dueling ring(8)", 1);
        TELEPORT_USAGE_ESTIMATES.put("Ring of wealth(5)", 1);
        TELEPORT_USAGE_ESTIMATES.put("Amulet of glory(6)", 1);
        TELEPORT_USAGE_ESTIMATES.put("Skills necklace(6)", 1);
        
        // Items to exclude from detection
        EXCLUDED_ITEMS.add("Coins");
        EXCLUDED_ITEMS.add("Looting bag");
        EXCLUDED_ITEMS.add("Rune pouch");
        EXCLUDED_ITEMS.add("Herb pouch");
        EXCLUDED_ITEMS.add("Seed box");
        EXCLUDED_ITEMS.add("Gem bag");
        EXCLUDED_ITEMS.add("Coal bag");
        EXCLUDED_ITEMS.add("Cannon barrels");
        EXCLUDED_ITEMS.add("Cannon base");
        EXCLUDED_ITEMS.add("Cannon stand");
        EXCLUDED_ITEMS.add("Cannon furnace");
        EXCLUDED_ITEMS.add("Dwarf cannon");
        EXCLUDED_ITEMS.add("Clue scroll");
    }
    
    /**
     * Gets an intelligent usage estimate for an item based on its category
     * @param itemName The name of the item
     * @param currentCount The current count the user has
     * @return Estimated usage per trip
     */
    public static int getUsageEstimate(String itemName, int currentCount) {
        // Check each category for exact matches
        if (FOOD_USAGE_ESTIMATES.containsKey(itemName)) {
            return FOOD_USAGE_ESTIMATES.get(itemName);
        }
        if (POTION_USAGE_ESTIMATES.containsKey(itemName)) {
            return POTION_USAGE_ESTIMATES.get(itemName);
        }
        if (RUNE_USAGE_ESTIMATES.containsKey(itemName)) {
            return RUNE_USAGE_ESTIMATES.get(itemName);
        }
        if (AMMO_USAGE_ESTIMATES.containsKey(itemName)) {
            return AMMO_USAGE_ESTIMATES.get(itemName);
        }
        if (TELEPORT_USAGE_ESTIMATES.containsKey(itemName)) {
            return TELEPORT_USAGE_ESTIMATES.get(itemName);
        }
        
        // Check for pattern matches
        String lowerName = itemName.toLowerCase();
        
        // Food patterns
        if (isFood(lowerName)) {
            return Math.max(1, Math.min(30, currentCount / 8)); // 1/8th of current, capped at 30
        }
        
        // Potion patterns
        if (isPotion(lowerName)) {
            return Math.max(1, Math.min(5, currentCount / 10)); // 1/10th of current, capped at 5
        }
        
        // Rune patterns
        if (isRune(lowerName)) {
            return Math.max(10, Math.min(200, currentCount / 5)); // 1/5th of current, between 10-200
        }
        
        // Ammunition patterns
        if (isAmmunition(lowerName)) {
            return Math.max(50, Math.min(500, currentCount / 3)); // 1/3rd of current, between 50-500
        }
        
        // Teleport patterns
        if (isTeleport(lowerName)) {
            return Math.max(1, Math.min(10, currentCount / 15)); // 1/15th of current, between 1-10
        }
        
        // Default fallback - 1/10th of current amount, minimum 1, maximum 50
        return Math.max(1, Math.min(50, currentCount / 10));
    }
    
    /**
     * Checks if an item should be excluded from restock detection
     * @param itemName The name of the item
     * @return True if the item should be excluded
     */
    public static boolean shouldExclude(String itemName) {
        if (EXCLUDED_ITEMS.contains(itemName)) {
            return true;
        }
        
        String lowerName = itemName.toLowerCase();
        
        // Exclude items with charges/variants in parentheses (except potions and jewelry)
        if (lowerName.contains("(") && !isPotion(lowerName) && !isJewelry(lowerName)) {
            return true;
        }
        
        // Exclude quest items
        if (lowerName.contains("clue") || lowerName.contains("casket") || 
            lowerName.contains("key") && !lowerName.contains("crystal key")) {
            return true;
        }
        
        // Exclude tools (unless they're consumable)
        if (isNonConsumableTool(lowerName)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the category of an item for display purposes
     * @param itemName The name of the item
     * @return Category string
     */
    public static String getItemCategory(String itemName) {
        String lowerName = itemName.toLowerCase();
        
        if (isFood(lowerName)) return "Food";
        if (isPotion(lowerName)) return "Potion";
        if (isRune(lowerName)) return "Rune";
        if (isAmmunition(lowerName)) return "Ammunition";
        if (isTeleport(lowerName)) return "Teleport";
        if (isJewelry(lowerName)) return "Jewelry";
        
        return "Other";
    }
    
    /**
     * Gets the category of an equipment item based on its slot and name
     * @param itemName The name of the item
     * @param equipmentSlot The equipment slot (e.g., "AMULET", "RING", "WEAPON", etc.)
     * @return Category string
     */
    public static String getEquipmentCategory(String itemName, String equipmentSlot) {
        String lowerName = itemName.toLowerCase();
        
        // Check if it's an amulet or ring with charges (contains parentheses)
        if (("AMULET".equals(equipmentSlot) || "RING".equals(equipmentSlot)) && itemName.contains("(")) {
            return "Jewelry";
        }
        
        // For other equipment slots, check if it's a known consumable type first
        if (isFood(lowerName)) return "Food";
        if (isPotion(lowerName)) return "Potion";
        if (isRune(lowerName)) return "Rune";
        if (isAmmunition(lowerName)) return "Ammunition";
        if (isTeleport(lowerName)) return "Teleport";
        if (isJewelry(lowerName)) return "Jewelry";
        
        // Default to "Gear" for equipment items
        return "Gear";
    }
    
    private static boolean isFood(String lowerName) {
        return lowerName.contains("fish") || lowerName.contains("shark") || lowerName.contains("karambwan") ||
               lowerName.contains("lobster") || lowerName.contains("tuna") || lowerName.contains("salmon") ||
               lowerName.contains("crab") || lowerName.contains("manta") || lowerName.contains("angler") ||
               lowerName.contains("bread") || lowerName.contains("cake") || lowerName.contains("pie") ||
               lowerName.contains("stew") || lowerName.contains("potato") || lowerName.contains("meat") ||
               lowerName.contains("chicken") || lowerName.contains("beef") || lowerName.contains("shrimp");
    }
    
    private static boolean isPotion(String lowerName) {
        // Check for explicit potion keywords first
        if (lowerName.contains("potion") || lowerName.contains("brew") || lowerName.contains("restore") ||
            lowerName.contains("prayer") || lowerName.contains("antipoison") || lowerName.contains("antidote") ||
            lowerName.contains("barbarian") || lowerName.contains("stamina") || lowerName.contains("energy")) {
            return true;
        }
        
        // Check for potion-specific stat boosters (but not equipment)
        // Only consider it a potion if it contains "potion" or has parentheses (indicating doses)
        if (lowerName.contains("(") && 
            (lowerName.contains("combat") || lowerName.contains("strength") || lowerName.contains("attack") || 
             lowerName.contains("defence") || lowerName.contains("ranging") || lowerName.contains("magic"))) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isRune(String lowerName) {
        return lowerName.contains(" rune") && !lowerName.contains("arrow") && !lowerName.contains("dart") &&
               !lowerName.contains("pickaxe") && !lowerName.contains("axe") && !lowerName.contains("sword") &&
               !lowerName.contains("armor") && !lowerName.contains("platebody") && !lowerName.contains("helmet");
    }
    
    private static boolean isAmmunition(String lowerName) {
        return lowerName.contains("arrow") || lowerName.contains("bolt") || lowerName.contains("dart") ||
               lowerName.contains("knife") || lowerName.contains("javelin") || lowerName.contains("cannonball") ||
               (lowerName.contains("rune") && (lowerName.contains("throw") || lowerName.contains("throwing")));
    }
    
    private static boolean isTeleport(String lowerName) {
        return lowerName.contains("teleport") || lowerName.contains("tablet") ||
               (lowerName.contains("tab") && (lowerName.contains("varrock") || lowerName.contains("lumbridge") ||
                lowerName.contains("falador") || lowerName.contains("camelot") || lowerName.contains("ardougne")));
    }
    
    private static boolean isJewelry(String lowerName) {
        return lowerName.contains("ring") || lowerName.contains("necklace") || lowerName.contains("amulet") ||
               lowerName.contains("bracelet") && (lowerName.contains("(") || lowerName.contains("charged"));
    }
    
    private static boolean isNonConsumableTool(String lowerName) {
        return (lowerName.contains("pickaxe") && !lowerName.contains("crystal")) ||
               (lowerName.contains("axe") && !lowerName.contains("crystal") && !lowerName.contains("throwing")) ||
               lowerName.contains("hammer") || lowerName.contains("chisel") || lowerName.contains("needle") ||
               lowerName.contains("saw") || lowerName.contains("spade") || lowerName.contains("rake") ||
               lowerName.contains("secateurs") || lowerName.contains("fishing rod") || lowerName.contains("net") ||
               lowerName.contains("harpoon") || lowerName.contains("bucket") || lowerName.contains("jug");
    }
} 
