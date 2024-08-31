package Scurrius;

import Scurrius.GUI.GUIManager;
import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.prayer.Prayers;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.TaskNode;
import org.dreambot.api.script.listener.GameTickListener;
import org.dreambot.api.script.listener.HumanMouseListener;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.items.Item;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@ScriptManifest(name = "ScarScurrius", author = "Scarfade", version = 1.0, description = "Scurrius Killer", category = Category.MONEYMAKING)
public class Scurrius extends AbstractScript implements PaintListener, HumanMouseListener {

    private Combat combat;
    private Map<String, Integer> supplies = new HashMap<>();
    private Map<String, String> equipment = new HashMap<>();
    private int setsToBuy = 0;
    private GrandExchangeTask geTask;
    private int arrowQuantity;
    private String mainAmmoType;
    private String specialAttackWeapon;
    private boolean specialAttackWeaponRequiresAmmo;
    private String specialAttackAmmoType;
    private static final String CONFIG_DIRECTORY = "C:/PkerConfigs/";
    private LootOverlay lootOverlay;
    private GUIManager gui;
    private Client client;
    private Prayers prayers;
    private Players players;
    private final List<TaskNode> tasks = new ArrayList<>();
    private boolean readyToFlick = false;
    private static final int MAX_LOGS = 5;
    private final List<String> logEntries = new LinkedList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean started = false;
    private ProjectileHandler projectileHandler;

    @Override
    public void onStart() {
        log("Script started!!");
        createConfigDirectory();
        combat = new Combat(lootOverlay, this,prayers);
        lootOverlay = new LootOverlay(combat);



        // GUI setup
        SwingUtilities.invokeLater(() -> {
            gui = new GUIManager(this, combat);
            gui.createAndShowGUI();
        });

        initializeTasks();
        started = false;
    }

    @Override
    public void onExit() {
        tasks.clear();
        log("Script stopped.");
    }
    public void setUserInputs(Map<String, Integer> supplies, Map<String, String> equipment, int setsToBuy, int arrowQuantity, String mainAmmoType, String specialAttackWeapon, boolean specialAttackWeaponRequiresAmmo, String specialAttackAmmoType, Combat combat) {
        this.supplies = supplies;
        this.equipment = equipment;
        this.setsToBuy = setsToBuy;
        this.arrowQuantity = arrowQuantity; // Store the arrow quantity
        this.mainAmmoType = mainAmmoType; // Store the main ammo type
        this.specialAttackWeapon = specialAttackWeapon; // Store the special attack weapon
        this.specialAttackWeaponRequiresAmmo = specialAttackWeaponRequiresAmmo; // Store if special attack weapon requires ammo
        this.specialAttackAmmoType = specialAttackAmmoType; // Store the special attack ammo type
        this.combat = combat;
        geTask = new GrandExchangeTask(this, supplies, equipment, setsToBuy, arrowQuantity, mainAmmoType, specialAttackWeapon, specialAttackWeaponRequiresAmmo, specialAttackAmmoType);
        started = true;
    }
    @Override
    public int onLoop() {
        if (!started) {
            return 1000;
        }
        long startTime = System.currentTimeMillis();
        if (!supplies.isEmpty() && !equipment.isEmpty()) {
            if (!readyToFlick) {
                if (geTask == null) {
                    if (Bank.open()) {
                        Sleep.sleepUntil(Bank::isOpen, 5000);
                        if (Bank.isOpen()) {
                            Map<String, Integer> missingItems = checkMissingItems();
                            Bank.close();
                            Sleep.sleepUntil(() -> !Bank.isOpen(), 5000);

                            if (!missingItems.isEmpty()) {
                                log("Missing items detected. Initiating Grand Exchange Task...");
                                List<String> itemNames = new ArrayList<>(missingItems.keySet());
                                List<Integer> quantities = new ArrayList<>(missingItems.values());
                                geTask = new GrandExchangeTask(this, supplies, equipment, setsToBuy, arrowQuantity, mainAmmoType, specialAttackWeapon, specialAttackWeaponRequiresAmmo, specialAttackAmmoType);
                            } else {
                                log("All required items are available. Proceeding to equip gear and withdraw supplies...");
                                equipGearAndWithdrawSupplies();
                                initializeTasks();
                            }
                        }
                    } else {
                        log("Failed to open bank. Retrying...");
                        return 1000;
                    }
                }

                if (geTask != null && !geTask.isCompleted()) {
                    int result = geTask.execute();
                    if (geTask.isCompleted()) {
                        log("Grand Exchange Task completed. Proceeding to equip gear and withdraw supplies...");
                        equipGearAndWithdrawSupplies();
                        readyToFlick = true;
                        initializeTasks();
                    }
                    return result;
                }
            } else {
                for (TaskNode task : tasks) {
                    if (task.accept()) {
                        log("Executing task: " + task.getClass().getSimpleName());
                        int result = task.execute();
                        long timeTaken = System.currentTimeMillis() - startTime;
                        log("Task executed in " + timeTaken + " ms");
                        return result;
                    }
                }
            }
        }

        return 1000;
    }

    public int getArrowQuantity() {
        return arrowQuantity;
    }

    public void setArrowQuantity(int arrowQuantity) {
        this.arrowQuantity = arrowQuantity;
    }

    public String getMainAmmoType() {
        return mainAmmoType;
    }

    public void setMainAmmoType(String mainAmmoType) {
        this.mainAmmoType = mainAmmoType;
    }

    public String getSpecialAttackWeapon() {
        return specialAttackWeapon;
    }

    public void setSpecialAttackWeapon(String specialAttackWeapon) {
        this.specialAttackWeapon = specialAttackWeapon;
    }

    public boolean isSpecialAttackWeaponRequiresAmmo() {
        return specialAttackWeaponRequiresAmmo;
    }

    public void setSpecialAttackWeaponRequiresAmmo(boolean specialAttackWeaponRequiresAmmo) {
        this.specialAttackWeaponRequiresAmmo = specialAttackWeaponRequiresAmmo;
    }

    public String getSpecialAttackAmmoType() {
        return specialAttackAmmoType;
    }

    public void setSpecialAttackAmmoType(String specialAttackAmmoType) {
        this.specialAttackAmmoType = specialAttackAmmoType;
    }
    public void msg(String message) {
        String timestamp = timeFormat.format(new Date());
        String logEntry = timestamp + " - " + message;

        logEntries.add(logEntry);
        if (logEntries.size() > MAX_LOGS) {
            logEntries.remove(0);
        }

        super.log(message);
    }

    private void initializeTasks() {
        tasks.clear();
        tasks.add(combat);
        log("Combat task added.");
    }
    void equipGearAndWithdrawSupplies() {
        if (Bank.open()) {
            Sleep.sleepUntil(Bank::isOpen, 5000);
            if (Bank.isOpen()) {
                equipGear();
                withdrawSupplies();
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), 5000);
            }
        } else {
            log("Failed to open bank for equipping and withdrawing. Retrying...");
        }
    }

    Map<String, Integer> checkMissingItems() {
        Map<String, Integer> missingItems = new HashMap<>();

        for (Map.Entry<String, Integer> entry : supplies.entrySet()) {
            String itemName = entry.getKey();
            int requiredQuantity = entry.getValue() * setsToBuy;

            if (itemName.equals("Arrows")) {
                String arrowType = equipment.get("Arrows");
                if (arrowType != null && !arrowType.equals("None")) {
                    int availableQuantity = Bank.count(arrowType);
                    if (availableQuantity < requiredQuantity) {
                        missingItems.put(arrowType, requiredQuantity - availableQuantity);
                    }
                }
            } else {
                int availableQuantity = Bank.count(itemName);
                if (availableQuantity < requiredQuantity) {
                    missingItems.put(itemName, requiredQuantity - availableQuantity);
                }
            }
        }

        for (Map.Entry<String, String> entry : equipment.entrySet()) {
            String slot = entry.getKey();
            String itemName = entry.getValue();
            if (!slot.equals("Arrows") && !itemName.isEmpty() && !itemName.equals("None") && !Bank.contains(itemName)) {
                missingItems.put(itemName, 1);
            }
        }

        return missingItems;
    }

    void restartProcess() {
        log("Restarting the process...");

        readyToFlick = false;
        tasks.clear();
        geTask = null;

        // Re-initialize Combat, ProjectileHandler, and LootOverlay
        combat = new Combat(lootOverlay, this,prayers);
        lootOverlay = new LootOverlay(combat);

        if (Bank.open()) {
            Sleep.sleepUntil(Bank::isOpen, 5000);
            if (Bank.isOpen()) {
                Map<String, Integer> missingItems = checkMissingItems();
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), 5000);

                if (!missingItems.isEmpty()) {
                    log("Missing items detected. Initiating GrandExchange Task...");
                    List<String> itemNames = new ArrayList<>(missingItems.keySet());
                    List<Integer> quantities = new ArrayList<>(missingItems.values());
                    geTask = new GrandExchangeTask(this, supplies, equipment, setsToBuy, arrowQuantity, mainAmmoType, specialAttackWeapon, specialAttackWeaponRequiresAmmo, specialAttackAmmoType);
                }
                int lowHealthThreshold = 55;
                if (Skills.getBoostedLevel(Skill.HITPOINTS) < lowHealthThreshold) {
                    restoreHealth();
                }
                else {
                    log("All required items are available. Proceeding to equip gear and withdraw supplies...");
                    equipGearAndWithdrawSupplies();
                    initializeTasks();
                }
            }
        } else {
            log("Failed to open bank during process restart. Retrying...");
        }
    }
    private void restoreHealth(){
        if (!Bank.isOpen()) {
            Bank.open();
        }
        Item shark = Inventory.get("Cooked karambwan");
        if (Bank.isOpen()){
            Bank.withdraw("Cooked karambwan",2);
            assert shark != null;
            shark.interact("Eat");
        }
    }
    private void equipGear() {
        log("Equipping gear...");

        for (Map.Entry<String, String> entry : equipment.entrySet()) {
            String slot = entry.getKey();
            String itemName = entry.getValue();

            if (itemName == null || itemName.isEmpty() || itemName.equals("None")) {
                log("No item specified for slot: " + slot);
                continue; // Skip if no valid item is specified
            }

            if (slot.equals("Arrows")) {
                // Handle arrows (or any main ammo type)
                handleAmmo(itemName, supplies.getOrDefault(itemName, 0));
            } else {
                // Equip regular gear
                equipItem(slot, itemName);
            }
        }

        // Handle special attack weapon and ammo if specified
        if (specialAttackWeapon != null && !specialAttackWeapon.equals("None")) {
            log("Equipping special attack weapon: " + specialAttackWeapon);
            equipItem("Special Attack Weapon", specialAttackWeapon);

            if (specialAttackWeaponRequiresAmmo && specialAttackAmmoType != null && !specialAttackAmmoType.isEmpty()) {
                log("Equipping special attack ammo: " + specialAttackAmmoType);
                handleAmmo(specialAttackAmmoType, supplies.getOrDefault(specialAttackAmmoType, 0));
            }
        }
    }

    // Method to handle equipping a specific item
    private void equipItem(String slot, String itemName) {
        if (!Inventory.contains(itemName)) {
            if (Bank.withdraw(itemName)) {
                Sleep.sleepUntil(() -> Inventory.contains(itemName), 5000);
            } else {
                log("Failed to withdraw: " + itemName);
                return;
            }
        }

        String action = (slot.equals("Weapon") || slot.equals("Special Attack Weapon")) ? "Wield" : "Wear";
        if (Inventory.interact(itemName, action)) {
            log("Equipped: " + itemName);
        } else {
            log("Failed to equip: " + itemName);
        }
    }

    // Method to handle ammo equipping
    private void handleAmmo(String ammoName, int requiredQuantity) {
        int currentAmmoCount = Inventory.count(ammoName) + Equipment.count(ammoName);
        int ammoToWithdraw = Math.max(0, requiredQuantity - currentAmmoCount);

        if (ammoToWithdraw > 0) {
            if (Bank.withdraw(ammoName, ammoToWithdraw)) {
                Sleep.sleepUntil(() -> Inventory.count(ammoName) + Equipment.count(ammoName) >= requiredQuantity, 5000);
            } else {
                log("Failed to withdraw ammo: " + ammoName);
                return;
            }
        }

        if (Inventory.count(ammoName) > 0) {
            if (Inventory.interact(ammoName, "Wield")) {
                log("Equipped: " + requiredQuantity + " x " + ammoName);
            } else {
                log("Failed to equip ammo: " + ammoName);
            }
        } else {
            log("No ammo found in inventory to equip: " + ammoName);
        }
    }


    private void withdrawSupplies() {
        log("Withdrawing supplies...");

        for (Map.Entry<String, Integer> entry : supplies.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            if (quantity > 0 && !itemName.equals("Arrows")) {
                if (Bank.contains(itemName)) {
                    Bank.withdraw(itemName, quantity);
                    log("Withdrew " + quantity + " x " + itemName);
                } else {
                    log("Failed to withdraw " + quantity + " x " + itemName + " (Not found in bank).");
                }
            }
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        int x = 40;
        int y = 350;
        int lineHeight = 20;
        int padding = 10;
        int width = 400;
        int height = lineHeight;

        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.setColor(new Color(0, 0, 0, 150));

        g.fillRoundRect(x, y - lineHeight, width, height, 20, 20);

        g.setColor(Color.WHITE);

        lootOverlay.onPaint(g);
    }

    private void createConfigDirectory() {
        File directory = new File(CONFIG_DIRECTORY);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                log("Created config directory: " + CONFIG_DIRECTORY);
            } else {
                log("Failed to create config directory: " + CONFIG_DIRECTORY);
            }
        }
    }

    public void setSupplies(Map<String, Integer> supplies) {
        this.supplies = supplies;
    }

    public void setEquipment(Map<String, String> equipment) {
        this.equipment = equipment;
    }

    public void setSetsToBuy(int setsToBuy) {
        this.setsToBuy = setsToBuy;
    }


    public void startScript() {
        log("Starting script with loaded configuration...");
        if (verifyConfiguration()) {
            initializeTasks();
            readyToFlick = true;
        } else {
            log("Invalid configuration. Please check your settings and try again.");
        }
    }

    private boolean verifyConfiguration() {
        if (supplies.isEmpty() || equipment.isEmpty() || setsToBuy <= 0) {
            log("Invalid configuration: supplies, equipment, or sets to buy are not properly set.");
            return false;
        }
        return true;
    }
}