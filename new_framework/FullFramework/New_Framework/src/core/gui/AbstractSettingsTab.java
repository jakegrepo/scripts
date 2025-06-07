package core.gui;

import core.context.ScriptContext;
import core.gui.components.SettingsCard;
import core.gui.style.StyleManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for script settings tabs
 */
public abstract class AbstractSettingsTab {
    protected final ScriptContext context;
    private final JPanel mainPanel;
    private final List<SettingsPanel> sections;
    private boolean initialized;

    protected AbstractSettingsTab(ScriptContext context) {
        this.context = context;
        this.mainPanel = new JPanel();
        this.sections = new ArrayList<>();
        this.initialized = false;

        setupPanel();
    }

    private void setupPanel() {
        // Use a more modern layout with better spacing
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        // Reduce vertical padding
        int reducedSpacing = StyleManager.COMPONENT_SPACING / 2;
        mainPanel.setBorder(BorderFactory.createEmptyBorder(reducedSpacing, StyleManager.COMPONENT_SPACING, reducedSpacing, StyleManager.COMPONENT_SPACING));
        mainPanel.setBackground(StyleManager.PANEL_BG);
    }

    protected void addSection(String title, SettingsPanel panel) {
        // Create a modern card for the section
        SettingsCard card = new SettingsCard(title);

        // Add the panel to the card
        card.setContent(panel);

        // Add reduced spacing between sections
        if (!sections.isEmpty()) {
            mainPanel.add(Box.createVerticalStrut(StyleManager.SECTION_SPACING / 2));
        }

        // Make the card fill the width but not stretch vertically
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        // Add the card to the main panel
        mainPanel.add(card);
        sections.add(panel);
    }

    protected void addSection(String title, JComponent component) {
        // Create a modern card for the section
        SettingsCard card = new SettingsCard(title);

        // Add the component to the card
        card.setContent(component);

        // Add reduced spacing between sections
        if (!sections.isEmpty()) {
            mainPanel.add(Box.createVerticalStrut(StyleManager.SECTION_SPACING / 2));
        }

        // Make the card fill the width but not stretch vertically
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        // Add the card to the main panel
        mainPanel.add(card);

        // Add to sections list if it's a settings panel
        if (component instanceof SettingsPanel) {
            sections.add((SettingsPanel) component);
        }
    }

    /**
     * Gets the main panel of this tab
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**

    /**
     * Initializes the tab's components
     */
    public void initialize() {
        if (!initialized) {
            createComponents();
            initialized = true;
        }
    }

    /**
     * Updates all sections with current config values
     */
    public void updateFromConfig() {
        debug("Updating tab from config: " + getTitle());

        // First reload settings in all sections
        sections.forEach(SettingsPanel::updateFromConfig);

        // Then call the tab-specific implementation to load any additional settings
        loadSettings();
    }

    /**
     * Loads settings from the config.
     * This method should be implemented by concrete tabs to load their specific settings.
     */
    public void loadSettings() {
        debug("Loading settings for tab: " + getTitle());
        // Default implementation does nothing
        // Concrete tabs should override this method
    }

    /**
     * Saves all section values to config
     */
    public void saveToConfig() {
        sections.forEach(SettingsPanel::saveToConfig);
    }

    /**
     * Saves the current settings to the config.
     * This method should be implemented by concrete tabs to save their specific settings.
     * It should call saveToConfig() to save all section values to the config.
     */
    public void saveSettings() {
        debug("Saving settings for tab: " + getTitle());
        saveToConfig();
    }

    /**
     * Creates the tab's components.
     * Must be implemented by concrete tabs.
     */
    public abstract void createComponents();

    /**
     * Resets all settings to their default values.
     */
    public abstract void resetToDefaults();

    /**
     * Gets the title of this tab
     */
    public String getTitle() {
        return context.getScript().getClass().getSimpleName();
    }

    protected File getProfilesDirectory() {
        // Get the script's data folder in DreamBot's settings directory
        File scriptDir = new File(
                System.getProperty("scripts.path"),
                "Dreambot/scripts/" + context.getScript().getClass().getSimpleName()
        );
        log("Script storage directory: " + scriptDir.getAbsolutePath());

        // Create profiles directory within the script's storage
        File profilesDir = new File(scriptDir, "profiles");
        log("Full profiles directory path: " + profilesDir.getAbsolutePath());

        // Create directory if it doesn't exist
        if (!profilesDir.exists()) {
            boolean created = profilesDir.mkdirs();
            log("Created profiles directory: " + created);
        }

        // Check directory status
        log("Profiles directory exists: " + profilesDir.exists());
        log("Profiles directory is directory: " + profilesDir.isDirectory());
        log("Profiles directory can write: " + profilesDir.canWrite());

        return profilesDir;
    }

    // Helper methods for logging
    protected void log(String message) {
        System.out.println("[" + context.getScript().getClass().getSimpleName() + "] " + message);
    }

    protected void debug(String message) {
        System.out.println("[DEBUG] [" + context.getScript().getClass().getSimpleName() + "] " + message);
    }

    protected void error(String message, Exception e) {
        System.err.println("[ERROR] [" + context.getScript().getClass().getSimpleName() + "] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
