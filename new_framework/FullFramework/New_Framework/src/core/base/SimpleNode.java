package core.base;


import core.config.ScriptConfig;
import core.node.TaskNode;
import core.state.ScriptState;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.impl.Condition;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.interactive.interact.Interactable;

/**
 * Enhanced SimpleNode with comprehensive utility methods
 */
public abstract class SimpleNode<T extends ScriptConfig> extends TaskNode {
    protected final T config;

    protected SimpleNode(String name, int priority, T config) {
        super(config.getContext(), name, priority);
        this.config = config;
    }

    // Status Management
    protected void status(String format, Object... args) {
        String newStatus = String.format(format, args);
        setStatus(newStatus);
        debug("[" + getName() + "] " + newStatus);
    }

    // Player Utilities
    protected Player getLocalPlayer() {
        return Players.getLocal();
    }

    protected boolean isPlayerAnimating() {
        return getLocalPlayer() != null && getLocalPlayer().isAnimating();
    }

    protected boolean isPlayerMoving() {
        return getLocalPlayer() != null && getLocalPlayer().isMoving();
    }

    // Inventory Utilities
    protected Inventory getInventory() {
        return null;
    }

    protected boolean isInventoryFull() {
        return Inventory.isFull();
    }

    protected boolean hasItems(String... items) {
        return Inventory.contains(items);
    }

    protected int getItemCount(String item) {
        return Inventory.count(item);
    }

    // Sleep Utilities
    protected void sleep(int min, int max) {
        Sleep.sleep(Calculations.random(min, max));
    }

    protected boolean sleepUntil(Condition condition, long timeout) {
        return Sleep.sleepUntil(condition, timeout);
    }

    // Add overload with polling parameter
    protected boolean sleepUntil(Condition condition, long timeout, long polling) {
        return Sleep.sleepUntil(condition, timeout, polling);
    }

    // Add overload with reset condition
    protected boolean sleepUntil(Condition condition, Condition reset, long timeout, long polling) {
        return Sleep.sleepUntil(condition, reset, timeout, polling);
    }

    // Walking Utilities
    protected boolean walkTo(Tile tile) {
        return Walking.walk(tile);
    }

    protected boolean walkTo(Area area) {
        return Walking.walk(area.getCenter());
    }

    // Enhanced Logging
    protected void logDebug(String format, Object... args) {
        debug("[" + getName() + "] " + String.format(format, args));
    }

    protected void logInfo(String format, Object... args) {
        log("[" + getName() + "] " + String.format(format, args));
    }

    protected void logError(String message, Exception e) {
        error("[" + getName() + "] " + message, e);
    }

    // State Management
    protected void setStateAndStatus(ScriptState state, String status) {
        setState(state);
        status(status);
    }

    // Common Validation Methods
    public boolean isReadyToExecute() {
        return getLocalPlayer() != null && !getLocalPlayer().isAnimating();
    }

    protected boolean shouldBank() {
        return isInventoryFull();
    }

    // Error Handling
    @Override
    public final int execute() {
        try {
            logDebug("Starting execution");
            
            if (shouldWait()) {
                logDebug("Wait condition met, skipping execution");
                return 1000;
            }

            int sleep = onExecute();
            logDebug("Execution complete, sleeping for %dms", sleep);
            return sleep;

        } catch (Exception e) {
            logError("Error during execution", e);
            return handleError(e);
        }
    }

    // Abstract Methods
    protected abstract int onExecute();
    
    protected boolean shouldWait() {
        return false;
    }
    
    protected int handleError(Exception e) {
        setStateAndStatus(ScriptState.ERROR, "Error: " + e.getMessage());
        return 5000;
    }

    // Common Node Utilities
    protected boolean interact(Interactable target, String action) {
        if (target == null) return false;
        
        logDebug("Attempting to %s with %s", action, target);
        boolean result = target.interact(action);
        
        if (result) {
            status("Interacting: %s", action);
        } else {
            logDebug("Failed to interact: %s", action);
        }
        return result;
    }

    // Resource Management
    protected void releaseResources() {
        logDebug("Releasing node resources");
        // Override in concrete nodes if needed
    }

    @Override
    public String toString() {
        return String.format("%s[status=%s, runtime=%dms]",
            getName(), getStatus(), getTimeSinceLastExecution());
    }

    /**
     * Cleanup method called when script is stopping or node is being removed
     * Override in child classes to handle cleanup
     */
    public void cleanup() {
        logDebug("Cleaning up node: " + getName());
    }
} 
