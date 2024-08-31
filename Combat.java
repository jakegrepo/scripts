package Scurrius;

import Pirates.Pirates;
import ScarSlayer.GUI.NPCInfo;
import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.interactive.*;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.prayer.Prayer;
import org.dreambot.api.methods.prayer.Prayers;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.script.TaskNode;
import org.dreambot.api.script.listener.GameTickListener;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.graphics.GraphicsObject;
import org.dreambot.api.wrappers.graphics.Projectile;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Combat extends TaskNode implements PaintListener, GameTickListener {


    private enum State {
        RESTOCK_AND_RETURN,
        CHECK_HEALTH,
        CHECK_PRAYER,
        CHECK_ENERGY_AND_POTIONS,
        ATTACK_SCURRIUS,
        HANDLE_ATTACK,
        HANDLE_DEATH,
        ENTER_SCURRIUS_LAIR,
        ATTACK_RATS,
        LOOT_AND_WAIT,
        IDLE
    }

    private boolean fightReady = false;
    private static final int MELEE_ATTACK_ANIMATION = 10693;
    private static final int ROCK_THROW_ANIMATION_1 = 10698;
    private static final int ROCK_THROW_ANIMATION_2 = 10699;
    private static final int FALLING_ROCKS_GRAPHICS_ID = 2644; // Make sure this is the correct ID
    private static final int GIANT_RAT_NPC_ID = 7223;
    public static final int SCURRIUS_NPC_ID = 7221;
    private static final int RANGED_PROJECTILE_ID = 2642;
    private static final int MAGIC_PROJECTILE_ID = 2640;
    private static final Area GRAND_EXCHANGE_AREA = new Area(3136, 3518, 3196, 3465);
    private static final Area LUMBRIDGE_BANK_AREA = new Area(3198, 3238, 3227, 3200);
    private static final Tile ENTRY_TILE = new Tile(3281, 9870, 0);
    private static final int ENTRY_OBJECT_ID = 14203; // Object ID to enter the Scurrius lair
    private final int lowHealthThreshold = 40;
    private final int lowPrayerThreshold = 20;
    private final int lowEnergyThreshold = 30;
    private final String foodName = "Shark";
    private final String[] prayerVariants = {
            "Prayer potion(4)", "Prayer potion(3)", "Prayer potion(2)", "Prayer potion(1)"
    };
    private final String[] staminaPotionVariants = {
            "Stamina potion(4)", "Stamina potion(3)", "Stamina potion(2)", "Stamina potion(1)"
    };
    private final String[] combatPotionVariants = {
            "Super combat potion(4)", "Super combat potion(3)", "Super combat potion(2)", "Super combat potion(1)"
    };
    private final String[] rangingPotionVariants = {
            "Ranging potion(4)", "Ranging potion(3)", "Ranging potion(2)", "Ranging potion(1)"
    };
    private final String[] pickupItems = {
            "Blood rune", "Death rune", "Chaos rune", "Elder chaos robe top", "Elder chaos hood",
            "Elder chaos robe top", "Battlestaff", "Adamant platebody", "Rune med helm", "Rune warhammer",
            "Rune battleaxe", "Rune longsword", "Rune sword", "Rune mace", "Dragon dagger", "Dragon longsword",
            "Dragon scimitar", "Blighted ancient ice sack", "Blighted anglerfish", "Blighted manta ray",
            "Blighted karambwan", "Blighted super restore(4)", "Zombie pirate key", "Cannonball", "Gold ore",
            "Adamant seeds", "Looting bag", "Teleport anchoring scroll", "Scurrius' spine"
    };
    private LootOverlay lootOverlay;
    private final Scurrius script;
    private boolean needsRestock = false;
    private int lastGameTick = -1;
public int npcDead = 0;
    public boolean prayerFlickingRunning = false;
    private State currentState = State.IDLE;
    private PrayerFlicker prayerFlicker;

    public Combat(LootOverlay lootOverlay, Scurrius script, Prayers prayers) {
        this.lootOverlay = lootOverlay;
        this.script = script;
        this.prayerFlicker = new PrayerFlicker(prayers);
    }

    public void setLootOverlay(LootOverlay lootOverlay) {
        this.lootOverlay = lootOverlay;
    }

    @Override
    public boolean accept() {
        return true;
    }

    @Override
    public int execute() {
        log("Executing with current state: " + currentState);
        Player localPlayer = Players.getLocal();
        detectFallingRocks(localPlayer);

        // Handle death scenario
        if (isInLumbridge()) {
            handleDeath();
            return 1000;
        }

        // Check for low supplies and handle restock
        if (isLowOnSupplies()) {
            teleportToGrandExchange();
            Sleep.sleep(1000);
            if (isAtGrandExchange()) {
                restockAndReturn();
            }
            return 1000;
        }

        // Enable run if required
        if (shouldEnableRun()) {
            Walking.toggleRun();
        }

        // Check health, prayer, and energy
        if (isHealthLow()) {
            checkHealth();
            return 300;
        }

        if (isPrayerLow()) {
            checkPrayer();
            return 300;
        }

        if (isEnergyLow()) {
            checkEnergyAndPotions();
            return 300;
        }

        // Manage prayer flicking based on state
        if (currentState == State.ATTACK_SCURRIUS && !prayerFlickingRunning) {
            startPrayerFlickingTask();
            log("Starting prayer flicking");
        }

        // Main state logic handling
        log("Current State: " + currentState);
        switch (currentState) {
            case RESTOCK_AND_RETURN:
                restockAndReturn();
                break;
            case CHECK_HEALTH:
                checkHealth();
                break;
            case CHECK_PRAYER:
                checkPrayer();
                break;
            case CHECK_ENERGY_AND_POTIONS:
                checkEnergyAndPotions();
                break;
            case ENTER_SCURRIUS_LAIR:
                enterScurriusLair();
                break;
            case ATTACK_SCURRIUS:
                attackScurrius();
                break;
            case LOOT_AND_WAIT:
                lootAndWait();
                break;
            case IDLE:
                determineNextState();
                break;
            default:
                break;
        }

        return 300;
    }

    @Override
    public void onGameTick() {
        if (Skills.getBoostedLevel(Skill.PRAYER) > 0 && prayerFlickingRunning) {
            prayerFlicker.onGameTick();
        }
        Player player= Players.getLocal();
        detectFallingRocks(player);
    }
    public void startPrayerFlickingTask() {
        prayerFlickingRunning = true;
        new Thread(() -> {
            while (prayerFlickingRunning) {
                if (Skills.getBoostedLevel(Skill.PRAYER) > 0) {
                    prayerFlicker.onGameTick();  // Use the PrayFlicker utility class
                } else {
                    Sleep.sleep(10);
                }
            }

        }).start();
    }
    private boolean isHealthLow() {
        return Skills.getBoostedLevel(Skill.HITPOINTS) < 40;
    }

    private boolean isPrayerLow() {
        return Skills.getBoostedLevel(Skill.PRAYER) < 20;
    }

    private boolean isEnergyLow() {
        return Walking.getRunEnergy() <= 30 && Inventory.contains("Stamina potion(4)");
    }

    private boolean isInLumbridge() {
        return LUMBRIDGE_BANK_AREA.contains(Players.getLocal());
    }
    public int getKillCount() {
        return npcDead;
    }
    private boolean isAtGrandExchange() {
        return GRAND_EXCHANGE_AREA.contains(Players.getLocal());
    }

    private boolean shouldEnableRun() {
        return !Walking.isRunEnabled() && Walking.getRunEnergy() > 10;
    }

    private void determineNextState() {
        if (isHealthLow()) {
            currentState = State.CHECK_HEALTH;
        } else if (isLowOnSupplies()) {
            teleportToGrandExchange();
            currentState = State.RESTOCK_AND_RETURN;
        } else if (isPrayerLow()) {
            currentState = State.CHECK_PRAYER;
        } else if (needsRestock && isAtGrandExchange()) {
            currentState = State.RESTOCK_AND_RETURN;
        } else if (fightReady) {
            currentState = State.ATTACK_SCURRIUS;
        } else {
            currentState = State.ENTER_SCURRIUS_LAIR;
        }
        log("State determined: " + currentState);
    }

    private void enterScurriusLair() {
        if (!ENTRY_TILE.equals(Players.getLocal().getTile())) {
            log("Walking to the Scurrius lair entry tile.");
            Walking.walk(ENTRY_TILE);
            Sleep.sleepUntil(() -> ENTRY_TILE.equals(Players.getLocal().getTile()), 2000);
        } else {
            GameObject entryObject = GameObjects.closest(ENTRY_OBJECT_ID);
            if (entryObject != null && entryObject.exists()) {
                if (entryObject.interact("Climb-through (private)")) {
                    log("Interacting with the Scurrius lair entrance.");
                    Sleep.sleepUntil(() -> entryObject != null && entryObject.exists() &&
                            !entryObject.hasAction("Climb-through (private)"), 5000);

                    if (!ENTRY_TILE.equals(Players.getLocal().getTile())) {
                        fightReady = true;
                        currentState = State.ATTACK_SCURRIUS;
                    } else {
                        log("Failed to enter Scurrius lair or action still available.");
                    }
                } else {
                    log("Failed to interact with the Scurrius lair entrance.");
                }
            } else {
                log("Failed to find the Scurrius lair entrance.");
            }
        }
    }

    private boolean isScurriusPresent() {
        NPC scurrius = NPCs.closest("Scurrius");
        log("NPCs present: " + NPCs.all());
        return scurrius != null && scurrius.exists();
    }

    private void attackScurrius() {
        if (handleGiantRats()) {
            log("Handling Giant Rats before attacking Scurrius.");
            return;
        }

        NPC scurrius = NPCs.closest("Scurrius");
        if (scurrius != null && scurrius.exists()) {
            if (!Players.getLocal().isInteracting(scurrius)) {
                if (scurrius.interact("Attack")) {
                    log("Attacking Scurrius.");
                } else {
                    log("Failed to attack Scurrius.");
                }
            }
        } else {
            log("Scurrius not found. Checking for loot or moving to next state.");

            GroundItem bigBones = GroundItems.closest("Big bones");
            if (bigBones != null && bigBones.exists()) {
                log("Big bones detected and Scurrius is not present. Moving to LOOT_AND_WAIT state.");
                currentState = State.LOOT_AND_WAIT;
            } else {
                log("No Big bones found. Waiting or determining next action.");
            }
        }
    }

    private void lootAndWait() {
        prayerFlickingRunning = false;
        if (Inventory.isFull()) {
            currentState = State.RESTOCK_AND_RETURN;
            return;
        }

        GroundItem bigBones = GroundItems.closest("Big bones");
        if (bigBones != null && bigBones.exists() && !isScurriusPresent()) {
            log("Big bones detected and Scurrius is not present. Entering LOOT_AND_WAIT.");
            npcDead++;
            if (lootItems()) {
                log("Looting items...");
                Sleep.sleep(500); // Add a slight delay after looting
            }
            return;
        }

        if (isScurriusPresent()) {
            log("Scurrius has respawned. Switching to attack state.");
            currentState = State.ATTACK_SCURRIUS;
            return;
        }

        if (Prayers.isQuickPrayerActive()) {
            Prayers.toggleQuickPrayer(false);
        }

        log("Waiting for Scurrius to respawn...");
        Sleep.sleep(5000);
    }

    private boolean lootItems() {
        GroundItem loot = GroundItems.closest(item -> Arrays.asList(pickupItems).contains(item.getName()));
        if (loot != null && loot.exists()) {
            if (loot.interact("Take")) {
                log("Looting item: " + loot.getName());
                Sleep.sleepUntil(() -> !loot.exists() || Inventory.isFull(), 2000);
                return true;
            }
        }
        return false;
    }

    private void detectFallingRocks(Player player) {
        List<GraphicsObject> graphicsObjects = Client.getGraphicsObjects();
        boolean matchFound = false;

        for (GraphicsObject g : graphicsObjects) {
            if (g.getID() == FALLING_ROCKS_GRAPHICS_ID && g.getTile().equals(player.getTile())) {
                log("Match Found: Falling rock detected at player’s tile.");
                matchFound = true;
                break;
            }
        }

        if (matchFound) {
            handleFallingRocks();
        } else {
            log("No falling rocks detected on player’s tile.");
        }
    }

    private void handleDeath() {
        prayerFlickingRunning = false;
        log("Player has died. Handling respawn...");
        if (LUMBRIDGE_BANK_AREA.contains(Players.getLocal())) {
            log("Player at Lumbridge bank. Attempting to recover gear...");
            Sleep.sleep(3000);
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 5000);
                Item ringOfWealth = Bank.get(item -> item != null && item.getName().contains("Ring of wealth"));
                if (ringOfWealth != null) {
                    if (Bank.withdraw(ringOfWealth.getName(), 1)) {
                        log("Withdrew " + ringOfWealth.getName() + " from the bank.");
                        Sleep.sleepUntil(() -> Inventory.contains(ringOfWealth.getName()), 5000);
                        if (ringOfWealth.interact("Wear")) {
                            log("Equipped " + ringOfWealth.getName() + ".");
                            Sleep.sleep(1000);
                            teleportToGrandExchange();
                        } else {
                            log("Failed to equip the Ring of Wealth.");
                        }
                    } else {
                        log("Failed to withdraw Ring of Wealth.");
                    }
                } else {
                    log("No Ring of Wealth found in the bank.");
                    Walking.walk(GRAND_EXCHANGE_AREA);
                }
                Bank.close();
            } else {
                log("Failed to open bank. Walking to Grand Exchange.");
                Walking.walk(GRAND_EXCHANGE_AREA);
            }

            currentState = State.RESTOCK_AND_RETURN;
        }
    }

    private void checkHealth() {
        if (Skills.getBoostedLevel(Skill.HITPOINTS) < lowHealthThreshold) {
            eatFood();
        } else {
            determineNextState();
        }
    }

    private void eatFood() {
        Item food = Inventory.get(foodName);
        if (food != null && food.hasAction("Eat") && food.interact("Eat")) {
            log("Eating food: " + foodName);
            Sleep.sleepUntil(() -> Skills.getBoostedLevel(Skill.HITPOINTS) > lowHealthThreshold, 3000);
        } else {
            log("No food found or failed to eat.");
        }
    }

    private void checkPrayer() {
        if (Skills.getBoostedLevel(Skill.PRAYER) < lowPrayerThreshold) {
            drinkRestore();
        } else {
            determineNextState();
        }
    }

    private void teleportToGrandExchange() {
        Player localPlayer = Players.getLocal();
        prayerFlickingRunning = false;
        if (!GRAND_EXCHANGE_AREA.contains(localPlayer)) {
            Item equippedRing = Equipment.getItemInSlot(EquipmentSlot.RING.getSlot());
            if (equippedRing == null || !equippedRing.getName().contains("Ring of wealth")) {
                Item inventoryRing = Inventory.get(item -> item != null && item.getName().contains("Ring of wealth"));
                if (inventoryRing != null) {
                    if (inventoryRing.interact("Wear")) {
                        log("Equipped " + inventoryRing.getName() + " from the inventory.");
                        Sleep.sleep(1000);
                        equippedRing = Equipment.getItemInSlot(EquipmentSlot.RING.getSlot());
                    } else {
                        log("Failed to equip " + inventoryRing.getName() + " from the inventory.");
                    }
                } else {
                    log("No Ring of Wealth found in inventory to equip.");
                }
            }

            if (equippedRing != null && equippedRing.getName().contains("Ring of wealth")) {
                if (equippedRing.interact("Grand Exchange")) {
                    log("Using " + equippedRing.getName() + " to teleport to the Grand Exchange.");
                    Sleep.sleepUntil(() -> GRAND_EXCHANGE_AREA.contains(localPlayer), 5000);
                    Sleep.sleep(5000);
                } else {
                    log("Failed to interact with " + equippedRing.getName() + ".");
                }
            } else {
                log("No Ring of Wealth found in equipment to teleport to Grand Exchange.");
            }
        } else {
            log("Already in the Grand Exchange area.");
            currentState = State.RESTOCK_AND_RETURN;
        }
    }

    private void checkEnergyAndPotions() {
        boolean needsAction = false;

        if (Walking.getRunEnergy() <= lowEnergyThreshold) {
            if (Inventory.contains(staminaPotionVariants)) {
                drinkStaminaPotion();
                needsAction = true;
            }
        }

        if (!hasRangedBoost() && Inventory.contains(rangingPotionVariants)) {
            drinkRangingPotion();
            needsAction = true;
        }

        if (!hasCombatBoost() && Inventory.contains(combatPotionVariants)) {
            drinkCombatPotion();
            needsAction = true;
        }

        if (!needsAction) {
            determineNextState();
        }
    }

    private void drinkStaminaPotion() {
        for (String potionName : staminaPotionVariants) {
            Item potion = Inventory.get(potionName);
            if (potion != null && potion.interact("Drink")) {
                log("Drinking " + potionName + " for stamina boost.");
                Sleep.sleep(2000);
                break;
            }
        }
    }

    private void drinkCombatPotion() {
        for (String potionName : combatPotionVariants) {
            Item potion = Inventory.get(potionName);
            if (potion != null && potion.interact("Drink")) {
                log("Drinking " + potionName + " to boost Combat.");
                Sleep.sleep(2000);
                break;
            }
        }
    }

    private void drinkRangingPotion() {
        for (String potionName : rangingPotionVariants) {
            Item potion = Inventory.get(potionName);
            if (potion != null && potion.interact("Drink")) {
                log("Drinking " + potionName + " to boost Ranged.");
                Sleep.sleep(2000);
                break;
            }
        }
    }

    private void drinkRestore() {
        for (String restoreName : prayerVariants) {
            Item restore = Inventory.get(restoreName);
            if (restore != null && restore.hasAction("Drink") && restore.interact("Drink")) {
                log("Drinking " + restoreName);
                Sleep.sleepUntil(() -> Skills.getBoostedLevel(Skill.PRAYER) > lowPrayerThreshold, 3000);
                break;
            }
        }
    }

    private void restockAndReturn() {
        prayerFlickingRunning = false;
        Player localPlayer = Players.getLocal();
        if (GRAND_EXCHANGE_AREA.contains(localPlayer)) {
            if (Prayers.isQuickPrayerActive()) {
                log("Prayers are currently active. Turning them off.");
                Prayers.toggleQuickPrayer(false);
                for (Prayer prayer : Prayer.values()) {
                    if (Prayers.isActive(prayer)) {
                        Prayers.toggle(false, prayer);
                        Sleep.sleep(50, 150);
                    }
                }
                log("Prayers turned off.");
            } else {
                log("No prayers are active.");
            }

            log("At the Grand Exchange. Initiating restock process.");
            if (Bank.open()) {
                if (Sleep.sleepUntil(Bank::isOpen, 25000)) {
                    if (Bank.depositAllItems()) {
                        log("Attempting to deposit all items...");
                        if (Sleep.sleepUntil(() -> Inventory.isEmpty(), 5000)) {
                            log("Successfully deposited all items.");
                        } else {
                            log("Failed to deposit all items. Retrying...");
                            Bank.depositAllItems();
                        }
                    }

                    if (Bank.depositAllEquipment()) {
                        log("Attempting to deposit all equipment...");
                        if (Sleep.sleepUntil(() -> Equipment.isEmpty(), 5000)) {
                            log("Successfully deposited all equipment.");
                        } else {
                            log("Failed to deposit all equipment. Retrying...");
                            Bank.depositAllEquipment();
                        }
                    }

                    Bank.close();
                    log("Finished restocking. Restarting the process.");
                    needsRestock = false;
                    script.restartProcess();
                    currentState = State.ENTER_SCURRIUS_LAIR;
                } else {
                    log("Failed to find or open the nearest bank.");
                }
            } else {
                log("Bank did not open in time, moving to Grand Exchange.");
            }
        }
    }

    private boolean needToRestock() {
        boolean outOfFood = !Inventory.contains(foodName);
        boolean outOfRestore = true;

        for (String restoreName : prayerVariants) {
            if (Inventory.contains(restoreName)) {
                outOfRestore = false;
                break;
            }
        }

        return outOfFood || outOfRestore;
    }

    private boolean hasCombatBoost() {
        return Skills.getBoostedLevel(Skill.STRENGTH) > Skills.getRealLevel(Skill.STRENGTH);
    }

    private boolean hasRangedBoost() {
        return Skills.getBoostedLevel(Skill.RANGED) > Skills.getRealLevel(Skill.RANGED);
    }

    @Override
    public int priority() {
        return 10;
    }


    private boolean isLowOnSupplies() {
        int foodCount = Inventory.count(foodName);
        int restoreCount = 0;
        for (String restoreName : prayerVariants) {
            restoreCount += Inventory.count(restoreName);
        }
        return foodCount < 3 || restoreCount < 1;
    }

    private void handleFallingRocks() {
        Tile safeTile = findSafeTile(Players.getLocal().getTile());
        if (safeTile != null) {
            if (Walking.walk(safeTile)) {
                log("Moving to safe tile to avoid falling rocks: " + safeTile);
                Sleep.sleepUntil(() -> Players.getLocal().getTile().equals(safeTile), 3000);
            } else {
                log("Failed to walk to safe tile: " + safeTile);
            }
        } else {
            log("No safe tile found to avoid falling rocks!");
        }
    }

    private Tile findSafeTile(Tile currentTile) {
        Area surroundingArea = currentTile.getArea(4);
        List<Tile> safeTiles = new ArrayList<>();

        for (Tile tile : surroundingArea.getTiles()) {
            if (!tile.equals(currentTile) && isSafeTile(tile)) {
                safeTiles.add(tile);
            }
        }

        if (!safeTiles.isEmpty()) {
            safeTiles.sort(Comparator.comparingInt(tile -> (int) tile.distance(Players.getLocal())));
            return safeTiles.get(0);
        }

        return null;
    }

    private boolean isSafeTile(Tile tile) {
        Area surroundingArea = tile.getArea(1);
        List<GraphicsObject> fallingRocks = GraphicsObjects.all(go ->
                go.getID() == FALLING_ROCKS_GRAPHICS_ID && surroundingArea.contains(go.getTile())
        );
        return fallingRocks.isEmpty();
    }

    private boolean handleGiantRats() {
        List<NPC> giantRats = NPCs.all(npc -> npc.getName().equals("Giant rat"));

        if (!giantRats.isEmpty()) {
            for (NPC rat : giantRats) {
                if (rat != null && rat.exists()) {
                    if (rat.interact("Attack")) {
                        Logger.log("Attacking Giant Rat: " + rat.getName());
                        Sleep.sleepUntil(() -> !rat.exists(), 600);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void log(String message) {
        Logger.log(message);
    }
}