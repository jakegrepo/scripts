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
        Logger.info("Training Cooking...");
        return 600;
    }
}
