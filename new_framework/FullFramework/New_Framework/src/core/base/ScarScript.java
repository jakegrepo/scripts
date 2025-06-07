package core.base;

import core.config.ScriptConfig;
import core.context.ContextProvider;
import core.context.ScriptContext;
import core.debug.TestingGUI;
import core.gui.AbstractSettingsTab;
import core.gui.FrameworkGUI;
import core.node.TaskNode;
import core.state.ScriptState;
import core.utils.ScriptLogger;
import org.dreambot.api.randoms.RandomEvent;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.utilities.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simplified base class for all ScarFramework scripts.
 * Combines functionality from ScarScript and ScriptFramework while maintaining all features.
 *
 * @param <T> The type of configuration class used by this script
 */
public abstract class ScarScript<T extends ScriptConfig> extends AbstractScript implements PaintListener {
    protected T config;
    protected final ScriptContext context;
    protected ScriptLogger logger;
    protected final ContextProvider provider;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private FrameworkGUI gui;
    private final String configKey;
    private ScriptState state;
    private Paint paint;

    private TestingGUI testingGUI;

    public ScarScript() {
        this.configKey = getClass().getSimpleName() + "_config";
        this.context = new ScriptContext(this);
        context.initializeLogger(getClass().getSimpleName());
        this.provider = new ContextProvider(context) {};
        this.logger = context.getLogger();
        initialize();
    }

    /**
     * Framework initialization
     */
    private void initialize() {
        try {
            logger.info("=== FRAMEWORK INITIALIZATION ===");
            context.setState(ScriptState.STOPPED);

            // Initialize config if provided
            T scriptConfig = getScriptConfig();
            if (scriptConfig != null) {
                this.config = scriptConfig;
                context.config().setConfig(configKey, config);

                // Load persisted values before showing the GUI
                try {
                    config.initialize();
                } catch (Exception e) {
                    logger.error("Failed to initialize config", e);
                }
            }

            // Initialize core systems
            context.initialize();

            // Store script metadata
            storeScriptMetadata();

            logger.info("Framework initialization complete");
        } catch (Exception e) {
            logger.error("Error during framework initialization", e);
            context.setState(ScriptState.ERROR);
        }
    }

    /**
     * Starts the script
     */
    public void start() {
        if (!started.get()) {
            started.set(true);
            paused.set(false);
            context.setState(ScriptState.RUNNING);
            onScriptStart();
            logger.info("Script started");
        }
    }

    /**
     * Stops the script
     */
    public void stop() {
        if (started.get()) {
            started.set(false);
            paused.set(false);
            context.setState(ScriptState.STOPPED);
            try {
                onScriptStop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.info("Script stopped");
        }
    }

    /**
     * Gets the current script state
     */
    public ScriptState getState() {
        return context.getState();
    }

    @Override
    public final void onStart() {
        try {
            // Initialize context first
            context.initialize();

            // Disable DeathsDoor random event handler since we handle death ourselves
            getRandomManager().disableSolver(RandomEvent.DEATHS_DOOR);
            Logger.log("Disabled DeathsDoor random event handler");

            // Initialize GUI if required
            if (requiresGUI()) {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        cleanupExistingGUI();
                        gui = new FrameworkGUI(context);

                        // Initialize script's settings tab
                        AbstractSettingsTab scriptTab = getScriptTab();
                        if (scriptTab != null) {
                            gui.addSettingsTab(scriptTab);
                        }

                        gui.show();
                    } catch (Exception e) {
                        logger.error("Error initializing GUI", e);
                    }
                });
            } else {
                // If no GUI required, start immediately
                onScriptStart();
                setState(ScriptState.RUNNING);
            }
        } catch (Exception e) {
            error("Error starting script", e);
            setState(ScriptState.ERROR);
        }
    }

    @Override
    public final int onLoop() {
        try {
            // Handle GUI completion
            if (requiresGUI() && !context.data().getGlobal("gui_completed", false)) {
                SwingUtilities.invokeLater(this::ensureGUIVisible);
                return 1000;
            }

            // Start script if not started
            if (!started.get() && (!requiresGUI() || context.data().getGlobal("gui_completed", false))) {
                start();
            }

            // Execute script logic
            return onScriptLoop();
        } catch (Exception e) {
            logger.error("Error in script loop", e);
            context.setState(ScriptState.ERROR);
            return 1000;
        }
    }

    private void ensureGUIVisible() {
        if (gui == null || !gui.isVisible()) {
            cleanupExistingGUI();
            gui = new FrameworkGUI(context);

            // Initialize script's settings tab
            AbstractSettingsTab scriptTab = getScriptTab();
            if (scriptTab != null) {
                gui.addSettingsTab(scriptTab);
            }

            gui.show();
        }
    }

    private void cleanupExistingGUI() {
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public final void onExit() {
        try {
            if (started.get()) {
                stop();
            }

            cleanupGUI();
        } catch (Exception e) {
            logger.error("Error during script exit", e);
        }
    }

    private void cleanupGUI() {
        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    gui.dispose();
                    gui = null;
                } catch (Exception e) {
                    logger.error("Error disposing GUI", e);
                }
            });
        }
    }

    private void storeScriptMetadata() {
        ScriptManifest manifest = getClass().getAnnotation(ScriptManifest.class);
        if (manifest != null) {
            context.data().setGlobal("script_name", manifest.name());
            context.data().setGlobal("script_version", manifest.version());
            context.data().setGlobal("script_author", manifest.author());
        }
    }

    // Simplified helper methods for child classes
    public void setState(ScriptState state) {
        context.setState(state);
    }

    protected void debug(String message) {
        logger.debug(message);
    }

    public void debug(String format, Object... args) {
        logger.debug(String.format(format, args));
    }

    protected void log(String message) {
        logger.info(message);
    }

    public void log(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    protected void error(String message, Exception e) {
        logger.error(message, e);
    }

    protected void setGlobalData(String key, Object value) {
        context.data().setGlobal(key, value);
    }

    // Abstract methods that must be implemented by child scripts
    protected abstract void onScriptStart();
    protected abstract void onScriptStop() throws InterruptedException;
    protected abstract int onScriptLoop();
    public abstract T getScriptConfig();
    public abstract boolean requiresGUI();
    protected abstract AbstractSettingsTab getScriptTab();

    // Getter for script context
    public ScriptContext getContext() {
        return context;
    }

    /**
     * Gets the script's configuration
     */
    public T getConfig() {
        return config;
    }

    // Delegate node management methods to provider
    protected void addNode(TaskNode node) {
        provider.addNode(node);
    }

    protected void clearNodes() {
        provider.clearNodes();
    }

    protected int getNodeCount() {
        return provider.getNodeCount();
    }

    public FrameworkGUI getGUI() {
        return gui;
    }

    public void openTestingGUI() {
        SwingUtilities.invokeLater(() -> {
            if (testingGUI == null) {
                testingGUI = new TestingGUI(context);
            }
            testingGUI.setVisible(true);
        });
    }

    public void closeTestingGUI() {
        if (testingGUI != null) {
            testingGUI = null;
        }
    }
}
