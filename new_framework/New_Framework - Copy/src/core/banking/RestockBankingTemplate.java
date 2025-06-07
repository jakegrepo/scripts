package core.banking;

import core.base.SimpleNode;
import core.config.ScriptConfig;
import core.grandexchange.RestockGrandExchangeNode;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

/**
 * Template for banking nodes that integrate with the restock system
 * This is a reference implementation that can be used as a guide for script-specific banking nodes
 */
public abstract class RestockBankingTemplate<T extends ScriptConfig> extends SimpleNode<T> {
    
    /**
     * Constructor
     * @param name The node name
     * @param priority The node priority
     * @param config The script config
     */
    public RestockBankingTemplate(String name, int priority, T config) {
        super(name, priority, config);
    }
    
    @Override
    public abstract boolean validate();
    
    @Override
    protected int onExecute() {
        // Open bank if not already open
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 3000);
            } else {
                Logger.warn("Failed to open bank");
                return 1000;
            }
        }
        
        if (Bank.isOpen()) {
            // Perform script-specific banking operations
            int result = performBankingOperations();
            if (result > 0) {
                return result;
            }
            
            // Check if restock is needed
            if (config.getRestockConfig().isEnabled()) {
                // Get the RestockGrandExchangeNode from the script
                RestockGrandExchangeNode restockNode = getContext().tasks().getNodes().stream()
                    .filter(node -> node instanceof RestockGrandExchangeNode)
                    .map(node -> (RestockGrandExchangeNode) node)
                    .findFirst().orElse(null);
                
                if (restockNode != null) {
                    // Force a restock check
                    restockNode.forceRestockCheck();
                    Logger.info("Triggered restock check during banking");
                    
                    // If restock is needed, let the RestockGrandExchangeNode handle it
                    if (!restockNode.isFinished()) {
                        // Close the bank to let the restock node take over
                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                        return 0;
                    }
                }
            }
            
            // Close the bank when done
            Bank.close();
            Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
        }
        
        return 1000;
    }
    
    /**
     * Perform script-specific banking operations
     * @return 0 if successful, or a sleep time in ms if more time is needed
     */
    protected abstract int performBankingOperations();
}
