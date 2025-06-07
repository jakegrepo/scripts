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
    private String method;

    protected AbstractSkillNode(String name, int priority, AIOConfig config, Skill skill) {
        super(name, priority, config);
        this.skill = skill;
    }

    @Override
    public boolean validate() {
        return skill != null &&
               org.dreambot.api.methods.skills.Skills.getRealLevel(skill) < config.getGoalLevel(skill);
    }

    /**
     * Chooses a training method based on config exclusions.
     */
    protected void selectMethod() {
        String[] methods = getAvailableMethods();
        if (methods == null || methods.length == 0) {
            method = null;
            return;
        }

        java.util.List<String> options = new java.util.ArrayList<>();
        for (String m : methods) {
            if (!config.isMethodExcluded(skill, m)) {
                options.add(m);
            }
        }
        if (options.isEmpty()) {
            method = null;
        } else {
            method = options.get(org.dreambot.api.methods.Calculations.random(0, options.size() - 1));
        }
    }

    protected String getMethod() {
        if (method == null) {
            selectMethod();
        }
        return method;
    }

    /**
     * Gets the list of available training methods for this skill.
     */
    protected abstract String[] getAvailableMethods();

    /**
     * Calculates the supplies required to reach the goal level for the chosen method.
     */
    public java.util.Map<String, Integer> getSuppliesForGoal() {
        String m = getMethod();
        int current = org.dreambot.api.methods.skills.Skills.getRealLevel(skill);
        int goal = config.getGoalLevel(skill);
        return calculateSupplies(m, current, goal);
    }

    protected java.util.Map<String, Integer> calculateSupplies(String method, int currentLevel, int targetLevel) {
        return java.util.Collections.emptyMap();
    }
}
