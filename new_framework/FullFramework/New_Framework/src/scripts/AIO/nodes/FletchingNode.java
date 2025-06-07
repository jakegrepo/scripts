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
        Logger.info("Training Fletching...");
        return 600;
    }
}
