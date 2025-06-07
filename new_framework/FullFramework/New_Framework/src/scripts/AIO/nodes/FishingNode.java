package scripts.AIO.nodes;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.utilities.Logger;
import scripts.AIO.AIOConfig;

/** Basic placeholder node for fishing. */
public class FishingNode extends AbstractSkillNode {
    public FishingNode(AIOConfig config) {
        super("Fishing", 1, config, Skill.FISHING);
    }

    @Override
    protected int onExecute() {
        String m = getMethod();
        Logger.info("Training Fishing via " + (m == null ? "default" : m));
        return 600;
    }

    @Override
    protected String[] getAvailableMethods() {
        return new String[]{"Net", "Fly", "Harpoon"};
    }

    @Override
    protected java.util.Map<String, Integer> calculateSupplies(String method, int currentLevel, int targetLevel) {
        int levels = Math.max(0, targetLevel - currentLevel);
        int perLevel = 20;
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("Fishing Bait", levels * perLevel);
        return map;
    }
        Logger.info("Training Fishing...");
        return 600;
    }
}
