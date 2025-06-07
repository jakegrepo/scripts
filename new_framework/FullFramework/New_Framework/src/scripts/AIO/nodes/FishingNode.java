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
        Logger.info("Training Fishing...");
        return 600;
    }
}
