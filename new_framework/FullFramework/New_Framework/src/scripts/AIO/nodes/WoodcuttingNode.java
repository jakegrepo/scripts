package scripts.AIO.nodes;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.utilities.Logger;
import scripts.AIO.AIOConfig;

public class WoodcuttingNode extends AbstractSkillNode {
    public WoodcuttingNode(AIOConfig config) {
        super("Woodcutting", 1, config, Skill.WOODCUTTING);
    }

    @Override
    protected int onExecute() {
        String m = getMethod();
        Logger.info("Training Woodcutting via " + (m == null ? "default" : m));
        return 600;
    }

    @Override
    protected String[] getAvailableMethods() {
        return new String[]{"Tree", "Oak", "Willow"};
    }

    @Override
    protected java.util.Map<String, Integer> calculateSupplies(String method, int currentLevel, int targetLevel) {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("Axe", 1);
        return map;
    }
        Logger.info("Training Woodcutting...");
        return 600;
    }
}
