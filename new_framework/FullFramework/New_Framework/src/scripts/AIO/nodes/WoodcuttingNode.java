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
        Logger.info("Training Woodcutting...");
        return 600;
    }
}
