package scripts.AIO.nodes;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.utilities.Logger;
import scripts.AIO.AIOConfig;

public class CookingNode extends AbstractSkillNode {
    public CookingNode(AIOConfig config) {
        super("Cooking", 1, config, Skill.COOKING);
    }

    @Override
    protected int onExecute() {
        String m = getMethod();
        Logger.info("Training Cooking via " + (m == null ? "default" : m));
        return 600;
    }

    @Override
    protected String[] getAvailableMethods() {
        return new String[]{"Fire", "Range"};
    }

    @Override
    protected java.util.Map<String, Integer> calculateSupplies(String method, int currentLevel, int targetLevel) {
        int levels = Math.max(0, targetLevel - currentLevel);
        int perLevel = 20;
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("Raw Fish", levels * perLevel);
        return map;
    }
        Logger.info("Training Cooking...");
        return 600;
    }
}
