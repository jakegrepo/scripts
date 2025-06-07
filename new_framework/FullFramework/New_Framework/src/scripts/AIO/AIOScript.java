package scripts.AIO;

import core.base.ScarScript;
import core.gui.AbstractSettingsTab;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScriptManifest(name = "ScarAIO", author = "Scarfade", version = 1.0, description = "All in one skilling", category = Category.MISC)
public class AIOScript extends ScarScript<AIOConfig> {
    private ScriptPaint paint;

    @Override
    protected void onScriptStart() {
        context.getLogger().info("Starting AIO script");
        initializeNodes();
        paint = new PaintBuilder(context)
                .setTitle("ScarAIO")
                .addRow("Status", () -> context.tasks().getActiveNode() != null ? context.tasks().getActiveNode().getName() : "Idle")
                .build();
        setState(ScriptState.RUNNING);
    }

    private void initializeNodes() {
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
}
