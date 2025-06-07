package scripts.AIO.nodes;

import core.base.SimpleNode;
import scripts.AIO.AIOConfig;
import org.dreambot.api.methods.skills.Skill;

/**
 * Base class for skill training nodes used by the AIO script.
 * Validation checks if the player has not reached the configured goal level.
 */
public abstract class AbstractSkillNode extends SimpleNode<AIOConfig> {
    private final Skill skill;

    protected AbstractSkillNode(String name, int priority, AIOConfig config, Skill skill) {
        super(name, priority, config);
        this.skill = skill;
    }

    @Override
    public boolean validate() {
        return skill != null &&
               org.dreambot.api.methods.skills.Skills.getRealLevel(skill) < config.getGoalLevel(skill);
    }
}
