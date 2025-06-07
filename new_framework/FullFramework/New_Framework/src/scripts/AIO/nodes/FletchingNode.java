package scripts.AIO.nodes;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.utilities.Logger;
import scripts.AIO.AIOConfig;

public class FletchingNode extends AbstractSkillNode {
    public FletchingNode(AIOConfig config) {
        super("Fletching", 1, config, Skill.FLETCHING);
    }

    @Override
    protected int onExecute() {
        String m = getMethod();
        Logger.info("Training Fletching via " + (m == null ? "default" : m));
        return 600;
    }

    @Override
    protected String[] getAvailableMethods() {
        return new String[]{"Longbow", "Shortbow"};
    }

    @Override
    protected java.util.Map<String, Integer> calculateSupplies(String method, int currentLevel, int targetLevel) {
        int levels = Math.max(0, targetLevel - currentLevel);
        int perLevel = 15;
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("Logs", levels * perLevel);
        return map;
    }
}
