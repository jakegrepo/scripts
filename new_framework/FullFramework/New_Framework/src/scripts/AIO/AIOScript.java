package scripts.AIO;

import core.base.ScarScript;
import core.gui.AbstractSettingsTab;
import core.paint.PaintBuilder;
import core.paint.ScriptPaint;
import core.state.ScriptState;
import core.node.TaskNode;
import core.paint.PaintBuilder;
import core.paint.ScriptPaint;
import core.state.ScriptState;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import scripts.AIO.gui.AIOSettingsTab;
import scripts.AIO.nodes.*;

import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScriptManifest(name = "ScarAIO", author = "Scarfade", version = 1.0, description = "All in one skilling", category = Category.MISC)
public class AIOScript extends ScarScript<AIOConfig> {
    private ScriptPaint paint;
    private final List<AbstractSkillNode> skillNodes = new ArrayList<>();
    private AbstractSkillNode activeNode;
    private long nextSwitch;

    @Override
    protected void onScriptStart() {
        context.getLogger().info("Starting AIO script");
        initializeNodes();
        calculateSupplies();
        paint = new PaintBuilder(context)
                .setTitle("ScarAIO")
                .addRow("Status", () -> activeNode != null ? activeNode.getName() : "Idle")
        paint = new PaintBuilder(context)
                .setTitle("ScarAIO")
                .addRow("Status", () -> context.tasks().getActiveNode() != null ? context.tasks().getActiveNode().getName() : "Idle")
                .build();
        setState(ScriptState.RUNNING);
    }

    private void initializeNodes() {
        skillNodes.clear();
        skillNodes.add(new FishingNode(getConfig()));
        skillNodes.add(new WoodcuttingNode(getConfig()));
        skillNodes.add(new CookingNode(getConfig()));
        skillNodes.add(new FletchingNode(getConfig()));
        Collections.shuffle(skillNodes);
    }

    private void calculateSupplies() {
        Map<String, Integer> totals = new HashMap<>();
        for (AbstractSkillNode node : skillNodes) {
            Map<String, Integer> need = node.getSuppliesForGoal();
            for (Map.Entry<String, Integer> e : need.entrySet()) {
                totals.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            context.getLogger().info("Need " + e.getValue() + " x " + e.getKey());
        }
        context.tasks().clearNodes();
        List<TaskNode> nodes = new ArrayList<>();
        nodes.add(new FishingNode(getConfig()));
        nodes.add(new WoodcuttingNode(getConfig()));
        nodes.add(new CookingNode(getConfig()));
        nodes.add(new FletchingNode(getConfig()));
        Collections.shuffle(nodes);
        nodes.forEach(context.tasks()::addNode);
    }

    @Override
    protected void onScriptStop() {
        context.getLogger().info("AIO script stopped");
    }

    @Override
    protected int onScriptLoop() {
        if (!getState().isRunnable()) {
            return 600;
        }

        if (System.currentTimeMillis() >= nextSwitch || activeNode == null || !activeNode.validate()) {
            activeNode = selectNextNode();
            nextSwitch = System.currentTimeMillis() + org.dreambot.api.methods.Calculations.random(60000, 180000);
        }

        if (activeNode != null) {
            return activeNode.execute();
        }
        setState(ScriptState.STOPPED);
        if (getState().isRunnable()) {
            return context.tasks().execute();
        }
        return 600;
    }

    @Override
    public AIOConfig getScriptConfig() {
        if (config == null) {
            config = new AIOConfig(getContext());
        }
        return config;
    }

    @Override
    public boolean requiresGUI() {
        return true;
    }

    @Override
    protected AbstractSettingsTab getScriptTab() {
        return new AIOSettingsTab(getContext());
    }

    @Override
    public void onPaint(Graphics2D g) {
        if (paint != null) {
            paint.render(g);
        }
    }

    /**
     * Selects the next skill node to execute based on remaining goals.
     */
    private AbstractSkillNode selectNextNode() {
        List<AbstractSkillNode> candidates = new ArrayList<>();
        for (AbstractSkillNode node : skillNodes) {
            if (node.validate()) {
                candidates.add(node);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(org.dreambot.api.methods.Calculations.random(0, candidates.size() - 1));
    }
}
