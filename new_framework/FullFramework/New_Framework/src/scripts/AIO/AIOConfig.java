package scripts.AIO;

import com.google.gson.annotations.Expose;
import core.config.ScriptConfig;
import core.context.ScriptContext;
import org.dreambot.api.methods.skills.Skill;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the AIO skilling script.
 * Stores goal levels and any excluded methods for each skill.
 */
public class AIOConfig extends ScriptConfig {
    @Expose
    private Map<String, Integer> goalLevels = new HashMap<>();

    @Expose
    private Map<String, String> excludedMethods = new HashMap<>();

    public AIOConfig(ScriptContext context) {
        super(context, "aio_config");
    }

    @Override
    protected void setDefaults() {
        goalLevels.clear();
        excludedMethods.clear();
    }

    public int getGoalLevel(Skill skill) {
        return goalLevels.getOrDefault(skill.name(), 1);
    }

    public void setGoalLevel(Skill skill, int level) {
        goalLevels.put(skill.name(), level);
        markDirty();
    }

    public String getExcludedMethods(Skill skill) {
        return excludedMethods.getOrDefault(skill.name(), "");
    }

    public void setExcludedMethods(Skill skill, String methods) {
        excludedMethods.put(skill.name(), methods == null ? "" : methods);
        markDirty();
    }

    /**
     * Checks if a method is excluded for the given skill.
     */
    public boolean isMethodExcluded(Skill skill, String method) {
        if (method == null) {
            return false;
        }
        String excluded = getExcludedMethods(skill);
        if (excluded.isEmpty()) {
            return false;
        }
        String[] parts = excluded.split(",");
        for (String part : parts) {
            if (part.trim().equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Integer> getGoalLevels() {
        return goalLevels;
    }
}
