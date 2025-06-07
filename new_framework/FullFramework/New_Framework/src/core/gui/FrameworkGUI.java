package core.gui;

import core.context.ScriptContext;
import core.gui.components.AnimatedButton;
import core.gui.components.GlassCard;
import core.gui.components.ModernComboBox;
import core.gui.style.ModernUITheme;
import core.state.ScriptState;
import core.base.ScarScript;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Completely redesigned main GUI frame for script settings
 * Features modern design, intuitive workflow, and excellent user experience
 */
public class FrameworkGUI {
    private final JFrame frame;
    private final List<AbstractSettingsTab> tabs;
    private final String scriptName;
    private final ScriptContext context;
    private final ScarScript<?> script;

    // Main layout components
    private JPanel mainContentPanel;
    private JPanel homePanel;
    private JPanel currentSettingsPanel;
    private String currentSettingsTitle;

    // Control components
    private JButton startButton;
    private JButton loadProfileButton;
    private JButton saveProfileButton;
    private JButton removeProfileButton;
    private ModernComboBox<String> profileSelector;

    // Status components
    private JLabel scriptStatusLabel;

    // Framework tabs
    private GeneralSettingsTab generalSettingsTab;
    private BreakSettingsTab breakSettingsTab;
    private AntiBanSettingsTab antiBanSettingsTab;
    private MulingSettingsTab mulingSettingsTab;
    private RestockSettingsTab restockSettingsTab;

    /**
     * Creates a new FrameworkGUI for the given script
     */
    public FrameworkGUI(ScriptContext context) {
        this.context = context;
        this.script = (ScarScript<?>) context.getScript();
        this.scriptName = script.getClass().getSimpleName();
        this.tabs = new ArrayList<>();

        // Create the main frame with modern styling
        frame = new JFrame(scriptName + " - Configuration");
        initializeFrame();

        // Initialize framework settings tabs
        initializeFrameworkTabs();

        // Set up the modern layout
        setupModernLayout();

        // Add window listener to handle closing
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
    }

    /**
     * Initializes the main frame with modern styling
     */
    private void initializeFrame() {
        // Apply modern UI theme to this frame only
        ModernUITheme.apply(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(ModernUITheme.MAIN_WINDOW_SIZE);
        frame.setMinimumSize(ModernUITheme.MIN_WINDOW_SIZE);
        frame.setLocationRelativeTo(null);
        frame.setBackground(ModernUITheme.BACKGROUND_DARKEST);

        // Set icon if available
        try {
            frame.setIconImage(new ImageIcon(getClass().getResource("/resources/icon.png")).getImage());
        } catch (Exception e) {
            // Icon not found, use default
            debug("Icon not found, using default");
        }
    }

    /**
     * Initializes all framework settings tabs
     */
    private void initializeFrameworkTabs() {
        generalSettingsTab = new GeneralSettingsTab(context);
        breakSettingsTab = new BreakSettingsTab(context);
        antiBanSettingsTab = new AntiBanSettingsTab(context);
        mulingSettingsTab = new MulingSettingsTab(context);
        restockSettingsTab = new RestockSettingsTab(context);

        // Add framework tabs in logical order
        addSettingsTab(generalSettingsTab);
        addSettingsTab(breakSettingsTab);
        addSettingsTab(antiBanSettingsTab);
        addSettingsTab(mulingSettingsTab);
        addSettingsTab(restockSettingsTab);
        
        // Note: Script-specific tabs are handled through ScriptSpecificSettingsPanel
        // and don't need to be added individually to the main tabs list
    }

    /**
     * Sets up the card-based layout system for switching between home and settings views
     */
    private void setupModernLayout() {
        // Create main content panel with BorderLayout
        mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mainContentPanel.setBackground(ModernUITheme.BACKGROUND_DARKEST);
        mainContentPanel.setOpaque(true);

        // Create dynamic header panel that changes based on current view
        JPanel headerPanel = createDynamicHeaderPanel();
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);

        // Create the card-based content area
        JPanel contentArea = createCardBasedContentArea();
        mainContentPanel.add(contentArea, BorderLayout.CENTER);

        // Create compact control panel
        JPanel controlPanel = createCompactControlPanel();
        mainContentPanel.add(controlPanel, BorderLayout.SOUTH);

        // Set as frame content
        frame.setContentPane(mainContentPanel);

        // Start on home view
        showHomeView();
    }

    /**
     * Creates the card-based content area that switches between home and settings views
     */
    private JPanel createCardBasedContentArea() {
        // Use CardLayout to switch between home and settings views
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.setBackground(ModernUITheme.BACKGROUND_DARKEST);

        // Create home panel with grid of settings cards
        homePanel = createHomePanel();
        cardPanel.add(homePanel, "HOME");

        // Settings panels will be added dynamically when needed
        return cardPanel;
    }

    /**
     * Creates the home panel with the grid of settings cards
     */
    private JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(ModernUITheme.BACKGROUND_DARKEST);
        homePanel.setBorder(new EmptyBorder(ModernUITheme.SPACING_MEDIUM,
                                           ModernUITheme.SPACING_MEDIUM,
                                           ModernUITheme.SPACING_MEDIUM,
                                           ModernUITheme.SPACING_MEDIUM));

        // Create the grid layout for settings cards (dynamic grid based on number of cards)
        int frameworkCards = 5; // General, Breaks, Anti-Ban, Muling, Restocking
        int scriptCards = hasScriptSpecificTabs() ? 1 : 0; // One unified script settings card
        int totalCards = frameworkCards + scriptCards;
        int columns = 3;
        int rows = (int) Math.ceil((double) totalCards / columns);
        JPanel gridPanel = new JPanel(new GridLayout(rows, columns, ModernUITheme.SPACING_MEDIUM, ModernUITheme.SPACING_MEDIUM));
        gridPanel.setBackground(ModernUITheme.BACKGROUND_DARKEST);

        // Add framework settings cards to the grid panel
        addFrameworkSettingsCards(gridPanel);

        homePanel.add(gridPanel, BorderLayout.CENTER);

        return homePanel;
    }

    /**
     * Creates a dynamic header panel that changes based on current view
     */
    private JPanel createDynamicHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Paint subtle gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, ModernUITheme.BACKGROUND_DARK,
                    0, getHeight(), ModernUITheme.BACKGROUND_DARKEST);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(ModernUITheme.ACCENT_PRIMARY.getRed(),
                         ModernUITheme.ACCENT_PRIMARY.getGreen(),
                         ModernUITheme.ACCENT_PRIMARY.getBlue(), 40)));
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 50)); // Slightly taller for navigation

        // Create navigation section (left side)
        JPanel navigationPanel = createHeaderNavigationPanel();
        headerPanel.add(navigationPanel, BorderLayout.WEST);

        // Add status indicator (right side)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ModernUITheme.SPACING_MEDIUM, ModernUITheme.SPACING_SMALL));
        statusPanel.setOpaque(false);

        scriptStatusLabel = new JLabel("Ready");
        scriptStatusLabel.setFont(ModernUITheme.FONT_SMALL);
        scriptStatusLabel.setForeground(ModernUITheme.ACCENT_PRIMARY);
        statusPanel.add(scriptStatusLabel);

        headerPanel.add(statusPanel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the header navigation panel with back button and title
     */
    private JPanel createHeaderNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ModernUITheme.SPACING_MEDIUM, ModernUITheme.SPACING_SMALL));
        navPanel.setOpaque(false);

        // Back button (initially hidden)
        JButton backButton = new JButton("< Back");
        backButton.setFont(ModernUITheme.FONT_SMALL);
        backButton.setForeground(ModernUITheme.ACCENT_PRIMARY);
        backButton.setBackground(new Color(0, 0, 0, 0));
        backButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setVisible(false); // Hidden on home view
        backButton.addActionListener(e -> showHomeView());

        // Add hover effect
        backButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                backButton.setContentAreaFilled(true);
                backButton.setBackground(new Color(ModernUITheme.ACCENT_PRIMARY.getRed(),
                                                  ModernUITheme.ACCENT_PRIMARY.getGreen(),
                                                  ModernUITheme.ACCENT_PRIMARY.getBlue(), 40));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                backButton.setContentAreaFilled(false);
            }
        });

        navPanel.add(backButton);

        // Dynamic title label
        JLabel titleLabel = new JLabel(scriptName + " Settings");
        titleLabel.setFont(ModernUITheme.FONT_HEADER);
        titleLabel.setForeground(ModernUITheme.TEXT_PRIMARY);
        navPanel.add(titleLabel);

        // Store references for dynamic updates
        navPanel.putClientProperty("backButton", backButton);
        navPanel.putClientProperty("titleLabel", titleLabel);

        return navPanel;
    }

    /**
     * Navigation methods for switching between views
     */
    private void showHomeView() {
        JPanel cardPanel = (JPanel) mainContentPanel.getComponent(1);
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, "HOME");

        // Update header
        updateHeaderForView("Home", false);
        currentSettingsPanel = null;
        currentSettingsTitle = null;

        debug("Switched to home view");
    }

    private void showSettingsView(String title, JPanel settingsPanel) {
        // Get the card panel
        JPanel cardPanel = (JPanel) mainContentPanel.getComponent(1);
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();

        // Remove existing settings panel if any
        if (currentSettingsPanel != null) {
            cardPanel.remove(currentSettingsPanel);
        }

        // Create wrapper for settings panel with proper styling
        JPanel settingsWrapper = createSettingsWrapper(settingsPanel);
        cardPanel.add(settingsWrapper, "SETTINGS");

        // Switch to settings view
        cardLayout.show(cardPanel, "SETTINGS");

        // Update header
        updateHeaderForView(title, true);
        currentSettingsPanel = settingsWrapper;
        currentSettingsTitle = title;

        debug("Switched to settings view: " + title);
    }

    /**
     * Creates a wrapper for settings panels with proper styling and scrolling
     */
    private JPanel createSettingsWrapper(JPanel settingsPanel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ModernUITheme.BACKGROUND_DARKEST);

        // Add settings panel in a scroll pane with smooth scrolling
        JScrollPane scrollPane = new JScrollPane(settingsPanel);
        scrollPane.setBackground(ModernUITheme.BACKGROUND_DARKEST);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ModernUITheme.BACKGROUND_DARKEST);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Improve scrolling performance and smoothness
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.setWheelScrollingEnabled(true);

        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * Updates the header for the current view
     */
    private void updateHeaderForView(String title, boolean showBackButton) {
        // Find the navigation panel in the header
        JPanel headerPanel = (JPanel) mainContentPanel.getComponent(0);
        JPanel navigationPanel = (JPanel) headerPanel.getComponent(0);

        // Get the stored components
        JButton backButton = (JButton) navigationPanel.getClientProperty("backButton");
        JLabel titleLabel = (JLabel) navigationPanel.getClientProperty("titleLabel");

        if (backButton != null) {
            backButton.setVisible(showBackButton);
        }

        if (titleLabel != null) {
            if (showBackButton) {
                titleLabel.setText(title);
            } else {
                titleLabel.setText(scriptName + " Settings");
            }
        }

        // Refresh the header
        headerPanel.revalidate();
        headerPanel.repaint();
    }

    /**
     * Adds framework settings cards to the grid panel
     */
    private void addFrameworkSettingsCards(JPanel gridPanel) {
        // Add script-specific settings cards FIRST to make them prominent
        addScriptSpecificCards(gridPanel);
        
        // Framework settings cards
        gridPanel.add(createFunctionalSettingsCard(
            "General", 
            "Basic script settings", 
            this::openGeneralSettings
        ));
        
        gridPanel.add(createFunctionalSettingsCard(
            "Breaks", 
            "Break management", 
            this::openBreakSettings
        ));
        
        gridPanel.add(createFunctionalSettingsCard(
            "Anti-Ban", 
            "Anti-ban features", 
            this::openAntiBanSettings
        ));
        
        gridPanel.add(createFunctionalSettingsCard(
            "Muling", 
            "Mule configuration", 
            this::openMulingSettings
        ));
        
        gridPanel.add(createFunctionalSettingsCard(
            "Restocking", 
            "GE restocking settings", 
            this::openRestockSettings
        ));
    }
    
    /**
     * Adds script-specific settings cards to the grid panel
     */
    private void addScriptSpecificCards(JPanel gridPanel) {
        // Check if script implements ScriptSettingsProvider and get tabs directly
        if (script instanceof ScriptSettingsProvider) {
            ScriptSettingsProvider provider = (ScriptSettingsProvider) script;
            List<AbstractSettingsTab> scriptTabs = provider.getAdditionalSettingsTabs();
            
            // Only add a script settings card if there are script-specific tabs
            if (!scriptTabs.isEmpty()) {
                String scriptName = getScriptDisplayName();
                String description = getScriptDescription();
                
                // Use enhanced script card that stands out from framework cards
                gridPanel.add(createEnhancedScriptSettingsCard(
                    scriptName + " Settings",
                    description,
                    () -> openScriptSpecificSettings(scriptTabs, scriptName, description)
                ));
            }
        }
    }
    
    /**
     * Determines if a tab is a framework tab (not script-specific)
     */
    private boolean isFrameworkTab(AbstractSettingsTab tab) {
        return tab instanceof GeneralSettingsTab ||
               tab instanceof BreakSettingsTab ||
               tab instanceof AntiBanSettingsTab ||
               tab instanceof MulingSettingsTab ||
               tab instanceof RestockSettingsTab;
    }
    
    /**
     * Opens the unified script-specific settings view with tabbed interface
     */
    private void openScriptSpecificSettings(List<AbstractSettingsTab> scriptTabs, 
                                             String scriptName, 
                                             String description) {
        // Create the script-specific settings panel with tabbed interface
        ScriptSpecificSettingsPanel settingsPanel = new ScriptSpecificSettingsPanel(
            context, scriptTabs, scriptName, description);
        
        // Update all tabs from config
        settingsPanel.updateFromConfig();
        
        // Update setup status
        settingsPanel.updateSetupStatus();
        
        // Show the settings view with a single scroll pane
        String title = scriptName + " Settings";
        showSettingsView(title, settingsPanel);
    }
    
    /**
     * Checks if there are any script-specific tabs available
     */
    private boolean hasScriptSpecificTabs() {
        // Check if the script implements ScriptSettingsProvider and has additional tabs
        if (script instanceof ScriptSettingsProvider) {
            ScriptSettingsProvider provider = (ScriptSettingsProvider) script;
            return !provider.getAdditionalSettingsTabs().isEmpty();
        }
        return false;
    }
    
    /**
     * Gets the script display name from the ScriptSettingsProvider interface
     */
    private String getScriptDisplayName() {
        if (script instanceof ScriptSettingsProvider) {
            return ((ScriptSettingsProvider) script).getScriptDisplayName();
        }
        return script.getClass().getSimpleName();
    }
    
    /**
     * Gets the script description from the ScriptSettingsProvider interface
     */
    private String getScriptDescription() {
        if (script instanceof ScriptSettingsProvider) {
            return ((ScriptSettingsProvider) script).getScriptDescription();
        }
        return "Script-specific configuration";
    }

    /**
     * Creates an enhanced script-specific settings card that stands out from framework cards
     */
    private GlassCard createEnhancedScriptSettingsCard(String title, String description, Runnable action) {
        // Create a completely custom card with vibrant script-specific styling
        GlassCard card = new GlassCard(title) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // Create rounded rectangle for the card
                java.awt.geom.RoundRectangle2D roundedRect = new java.awt.geom.RoundRectangle2D.Double(
                        0, 0, width - 1, height - 1, ModernUITheme.CARD_RADIUS, ModernUITheme.CARD_RADIUS);

                // Draw extremely dramatic shadow for depth
                for (int i = 0; i < 12; i++) {
                    float opacity = 0.6f * (1.0f - (float) i / 12);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                    g2.setColor(new Color(ModernUITheme.ACCENT_PRIMARY.getRed(),
                                         ModernUITheme.ACCENT_PRIMARY.getGreen(),
                                         ModernUITheme.ACCENT_PRIMARY.getBlue(), 150));
                    g2.fill(new java.awt.geom.RoundRectangle2D.Double(
                            i + 4, i + 4, width - i * 2 - 8, height - i * 2 - 8,
                            ModernUITheme.CARD_RADIUS, ModernUITheme.CARD_RADIUS));
                }

                // Reset composite for main background
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                // Draw vibrant, dramatic gradient background with much higher opacity
                GradientPaint bgGradient = new GradientPaint(
                    0, 0, new Color(ModernUITheme.ACCENT_PRIMARY.getRed(),
                                   ModernUITheme.ACCENT_PRIMARY.getGreen(),
                                   ModernUITheme.ACCENT_PRIMARY.getBlue(), 120),
                    0, height, new Color(Math.min(255, ModernUITheme.ACCENT_PRIMARY.getRed() + 40),
                                        Math.min(255, ModernUITheme.ACCENT_PRIMARY.getGreen() + 40),
                                        Math.min(255, ModernUITheme.ACCENT_PRIMARY.getBlue() + 40), 80));
                g2.setPaint(bgGradient);
                g2.fill(roundedRect);

                // Add darker base background for contrast and readability
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g2.setColor(new Color(Math.min(255, ModernUITheme.BACKGROUND_MEDIUM.getRed() + 20),
                                     Math.min(255, ModernUITheme.BACKGROUND_MEDIUM.getGreen() + 20),
                                     Math.min(255, ModernUITheme.BACKGROUND_MEDIUM.getBlue() + 20)));
                g2.fill(roundedRect);

                // Draw extremely prominent accent border
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g2.setStroke(new BasicStroke(6.0f));
                g2.setColor(ModernUITheme.ACCENT_PRIMARY);
                g2.draw(roundedRect);

                // Add inner highlight
                g2.setStroke(new BasicStroke(2.0f));
                java.awt.geom.RoundRectangle2D innerRect = new java.awt.geom.RoundRectangle2D.Double(
                        3, 3, width - 7, height - 7, ModernUITheme.CARD_RADIUS - 2, ModernUITheme.CARD_RADIUS - 2);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.draw(innerRect);

                g2.dispose();
            }
        };

        JPanel content = new JPanel(new BorderLayout(0, ModernUITheme.SPACING_SMALL));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(ModernUITheme.SPACING_SMALL + 2,
                                         ModernUITheme.SPACING_SMALL + 2,
                                         ModernUITheme.SPACING_SMALL + 2,
                                         ModernUITheme.SPACING_SMALL + 2));

        // Enhanced description with better text wrapping and styling
        String wrappedDescription = wrapText(description, 35); // Wrap at ~35 characters
        JLabel descLabel = new JLabel("<html><div style='text-align: center; color: rgb(255,255,255); font-weight: bold; line-height: 1.3; text-shadow: 1px 1px 2px rgba(0,0,0,0.5);'>" +
                                     wrappedDescription + "</div></html>");
        descLabel.setFont(ModernUITheme.FONT_SMALL.deriveFont(Font.BOLD, 13f));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        // Set preferred size to ensure proper layout
        descLabel.setPreferredSize(new Dimension(200, 50));
        content.add(descLabel, BorderLayout.CENTER);

        // Enhanced configure button for script card
        JButton configureButton = new JButton("Configure Script");
        configureButton.setFont(ModernUITheme.FONT_SMALL.deriveFont(Font.BOLD, 12f));
        
        // Create a brighter version of the accent color for the button
        Color brighterAccent = new Color(
            Math.min(255, ModernUITheme.ACCENT_PRIMARY.getRed() + 40),
            Math.min(255, ModernUITheme.ACCENT_PRIMARY.getGreen() + 40),
            Math.min(255, ModernUITheme.ACCENT_PRIMARY.getBlue() + 40)
        );
        
        configureButton.setBackground(brighterAccent);
        configureButton.setForeground(Color.WHITE);
        configureButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(brighterAccent, 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        configureButton.setFocusPainted(false);
        configureButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect with smooth transitions
        configureButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                Color hoverColor = new Color(
                    Math.min(255, brighterAccent.getRed() + 25),
                    Math.min(255, brighterAccent.getGreen() + 25),
                    Math.min(255, brighterAccent.getBlue() + 25)
                );
                configureButton.setBackground(hoverColor);
                configureButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(hoverColor, 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
                configureButton.setText("Open Settings");
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                configureButton.setBackground(brighterAccent);
                configureButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(brighterAccent, 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
                configureButton.setText("Configure Script");
            }
        });

        // Add the action listener
        configureButton.addActionListener(e -> {
            try {
                debug("Opening enhanced script settings for: " + title);
                action.run();
            } catch (Exception ex) {
                debug("Error opening script settings: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame,
                    "Error opening script settings: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(configureButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        card.addContent(content);
        
        debug("Created enhanced script card for: " + title);
        return card;
    }

    /**
     * Wraps text to fit within the specified character width
     */
    private String wrapText(String text, int maxWidth) {
        if (text.length() <= maxWidth) {
            return text;
        }
        
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (result.length() > 0) {
                    result.append("<br>");
                }
                result.append(currentLine);
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            if (result.length() > 0) {
                result.append("<br>");
            }
            result.append(currentLine);
        }
        
        return result.toString();
    }

    /**
     * Creates a functional settings card for the grid layout
     */
    private GlassCard createFunctionalSettingsCard(String title, String description, Runnable action) {
        GlassCard card = new GlassCard(title);

        JPanel content = new JPanel(new BorderLayout(0, ModernUITheme.SPACING_SMALL));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(ModernUITheme.SPACING_SMALL,
                                         ModernUITheme.SPACING_SMALL,
                                         ModernUITheme.SPACING_SMALL,
                                         ModernUITheme.SPACING_SMALL));

        // Description
        JLabel descLabel = new JLabel("<html><div style='text-align: center; color: rgb(180,180,180);'>" +
                                     description + "</div></html>");
        descLabel.setFont(ModernUITheme.FONT_SMALL);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(descLabel, BorderLayout.CENTER);

        // Configure button with action
        JButton configureButton = new JButton("Configure");
        configureButton.setFont(ModernUITheme.FONT_SMALL);
        configureButton.setBackground(ModernUITheme.ACCENT_PRIMARY);
        configureButton.setForeground(Color.WHITE);
        configureButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        configureButton.setFocusPainted(false);

        // Add the action listener
        configureButton.addActionListener(e -> {
            try {
                action.run();
            } catch (Exception ex) {
                debug("Error opening settings: " + ex.getMessage());
                JOptionPane.showMessageDialog(frame,
                    "Error opening settings: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(configureButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        card.addContent(content);
        return card;
    }

    /**
     * Action handlers for opening settings panels in full-screen view
     */
    private void openGeneralSettings() {
        debug("Opening General Settings");
        if (generalSettingsTab != null) {
            showSettingsView("General Settings", generalSettingsTab.getPanel());
        }
    }

    private void openBreakSettings() {
        debug("Opening Break Settings");
        if (breakSettingsTab != null) {
            showSettingsView("Break Settings", breakSettingsTab.getPanel());
        }
    }

    private void openAntiBanSettings() {
        debug("Opening Anti-Ban Settings");
        if (antiBanSettingsTab != null) {
            showSettingsView("Anti-Ban Settings", antiBanSettingsTab.getPanel());
        }
    }

    private void openMulingSettings() {
        debug("Opening Muling Settings");
        if (mulingSettingsTab != null) {
            showSettingsView("Muling Settings", mulingSettingsTab.getPanel());
        }
    }

    private void openRestockSettings() {
        debug("Opening Restock Settings");
        if (restockSettingsTab != null) {
            showSettingsView("Restock Settings", restockSettingsTab.getPanel());
        }
    }

    /**
     * Creates a compact control panel that doesn't waste vertical space
     */
    private JPanel createCompactControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(ModernUITheme.SPACING_MEDIUM, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Paint subtle gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, ModernUITheme.BACKGROUND_DARK,
                    0, getHeight(), ModernUITheme.BACKGROUND_DARKEST);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };

        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(ModernUITheme.ACCENT_PRIMARY.getRed(),
                         ModernUITheme.ACCENT_PRIMARY.getGreen(),
                         ModernUITheme.ACCENT_PRIMARY.getBlue(), 40)),
            BorderFactory.createEmptyBorder(ModernUITheme.SPACING_SMALL,
                                          ModernUITheme.SPACING_MEDIUM,
                                          ModernUITheme.SPACING_SMALL,
                                          ModernUITheme.SPACING_MEDIUM)
        ));
        controlPanel.setOpaque(false);
        controlPanel.setPreferredSize(new Dimension(0, 55)); // Reduced height

        // Create compact profile management panel
        JPanel profilePanel = createCompactProfilePanel();
        controlPanel.add(profilePanel, BorderLayout.WEST);

        // Create compact action buttons panel
        JPanel actionPanel = createCompactActionPanel();
        controlPanel.add(actionPanel, BorderLayout.EAST);

        return controlPanel;
    }

    /**
     * Creates a compact profile management panel
     */
    private JPanel createCompactProfilePanel() {
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, ModernUITheme.SPACING_SMALL, 0));
        profilePanel.setOpaque(false);

        // Profile label
        JLabel profileLabel = new JLabel("Profile:");
        profileLabel.setFont(ModernUITheme.FONT_SMALL);
        profileLabel.setForeground(ModernUITheme.TEXT_PRIMARY);
        profilePanel.add(profileLabel);

        // Profile selector
        profileSelector = new ModernComboBox<>();
        profileSelector.setPreferredSize(new Dimension(140, 28)); // Reduced size
        profilePanel.add(profileSelector);

        // Compact profile management buttons
        loadProfileButton = new AnimatedButton("Load", AnimatedButton.ButtonStyle.GHOST);
        saveProfileButton = new AnimatedButton("Save", AnimatedButton.ButtonStyle.GHOST);
        removeProfileButton = new AnimatedButton("Del", AnimatedButton.ButtonStyle.GHOST);

        Dimension buttonSize = new Dimension(50, 28); // Smaller buttons
        for (JButton button : new JButton[]{loadProfileButton, saveProfileButton, removeProfileButton}) {
            button.setPreferredSize(buttonSize);
            button.setFont(ModernUITheme.FONT_SMALL);
            profilePanel.add(button);
        }

        // Add action listeners
        loadProfileButton.addActionListener(e -> handleLoadProfile());
        saveProfileButton.addActionListener(e -> handleSaveProfile());
        removeProfileButton.addActionListener(e -> handleRemoveProfile());

        return profilePanel;
    }

    /**
     * Creates a compact action buttons panel
     */
    private JPanel createCompactActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, ModernUITheme.SPACING_SMALL, 0));
        actionPanel.setOpaque(false);

        // Start button
        startButton = new AnimatedButton("Start Script", AnimatedButton.ButtonStyle.PRIMARY);
        startButton.setPreferredSize(new Dimension(100, 32)); // Compact size
        startButton.setFont(ModernUITheme.FONT_REGULAR);
        startButton.addActionListener(e -> handleStart());
        actionPanel.add(startButton);

        return actionPanel;
    }

    /**
     * Adds a settings tab to the GUI (updated for new grid-based design)
     */
    public void addSettingsTab(AbstractSettingsTab tab) {
        if (tab == null) {
            return;
        }

        tabs.add(tab);
        // Note: In the new design, tabs are represented as cards in the grid
        // The actual tab content will be shown in modal dialogs or separate windows
        debug("Added settings tab: " + tab.getTitle());

        // Refresh home panel to include new script-specific settings
        if (!isFrameworkTab(tab)) {
            SwingUtilities.invokeLater(this::refreshHomePanel);
        }
    }
    
    /**
     * Refreshes the home panel to reflect any changes in available settings tabs
     */
    private void refreshHomePanel() {
        if (homePanel != null) {
            // Recreate the home panel with updated grid
            JPanel newHomePanel = createHomePanel();
            
            // Replace the old home panel
            Container parent = homePanel.getParent();
            if (parent instanceof JPanel) {
                JPanel cardPanel = (JPanel) parent;
                cardPanel.remove(homePanel);
                cardPanel.add(newHomePanel, "HOME");
                homePanel = newHomePanel;
                cardPanel.revalidate();
                cardPanel.repaint();
            }
        }
    }

    /**
     * Shows the GUI and brings it to front
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            // Initialize all tabs first and load saved settings
            for (AbstractSettingsTab tab : tabs) {
                tab.initialize();
                tab.createComponents();
                tab.updateFromConfig();
            }

            // Update profile selector and configuration progress
            updateProfileSelector();
            updateConfigurationStatus();

            // Set size and show
            frame.setSize(ModernUITheme.MAIN_WINDOW_SIZE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();

            // Set initial state
            context.setState(ScriptState.STOPPED);
            updateControlButtons();

            debug("Framework GUI shown successfully");
        });
    }

    /**
     * Updates the profile selector with available profiles
     */
    private void updateProfileSelector() {
        profileSelector.removeAllItems();

        // Add default profile
        profileSelector.addItem("Default");

        // Add available profiles
        for (String profile : script.getConfig().getAvailableProfiles()) {
            profileSelector.addItem(profile);
        }
    }

    /**
     * Updates the control buttons based on script state
     */
    private void updateControlButtons() {
        ScriptState state = context.getState();
        startButton.setText(state == ScriptState.RUNNING ? "Stop" : "Start");
    }

    /**
     * Saves all settings from all tabs
     */
    private void saveAllSettings() {
        debug("Saving all settings from all tabs");
        for (AbstractSettingsTab tab : tabs) {
            try {
                debug("Saving settings for tab: " + tab.getTitle());
                tab.saveSettings();
            } catch (Exception e) {
                error("Error saving settings for tab: " + tab.getTitle(), e);
            }
        }

        // Save config to file
        try {
            debug("Saving config to file");
            script.getConfig().saveConfig();
        } catch (Exception e) {
            error("Error saving config to file", e);
        }
    }

    /**
     * Handles the start button click
     */
    private void handleStart() {
        saveAllSettings();
        context.data().setGlobal("gui_completed", true);
        script.start();
        frame.dispose();
    }

    /**
     * Handles the load profile button click
     */
    private void handleLoadProfile() {
        String selectedProfile = (String) profileSelector.getSelectedItem();
        if (selectedProfile != null && !selectedProfile.isEmpty()) {
            try {
                debug("Loading profile: " + selectedProfile);

                // Load profile
                script.getConfig().loadProfile(selectedProfile);

                // Update all tabs with loaded settings
                for (AbstractSettingsTab tab : tabs) {
                    debug("Updating tab: " + tab.getTitle());
                    tab.updateFromConfig();
                }

                // Show success message
                JOptionPane.showMessageDialog(frame,
                                            "Profile '" + selectedProfile + "' loaded successfully!",
                                            "Profile Loaded",
                                            JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // Show error message
                JOptionPane.showMessageDialog(frame,
                                            "Error loading profile: " + e.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                error("Error loading profile: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Handles the save profile button click
     */
    private void handleSaveProfile() {
        String profileName = JOptionPane.showInputDialog(frame,
                                                       "Enter profile name:",
                                                       "Save Profile",
                                                       JOptionPane.PLAIN_MESSAGE);

        if (profileName != null && !profileName.isEmpty()) {
            try {
                debug("Saving profile: " + profileName);

                // Save all settings first
                saveAllSettings();

                // Mark config as dirty to ensure it's saved
                script.getConfig().markDirty();

                // Save profile
                script.getConfig().saveProfile(profileName);

                // Update profile selector
                updateProfileSelector();
                profileSelector.setSelectedItem(profileName);

                // Show success message
                JOptionPane.showMessageDialog(frame,
                                            "Profile '" + profileName + "' saved successfully!",
                                            "Profile Saved",
                                            JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // Show error message
                JOptionPane.showMessageDialog(frame,
                                            "Error saving profile: " + e.getMessage(),
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                error("Error saving profile: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Handles the remove profile button click
     */
    private void handleRemoveProfile() {
        String selectedProfile = (String) profileSelector.getSelectedItem();
        if (selectedProfile != null && !selectedProfile.equals("Default")) {
            int result = JOptionPane.showConfirmDialog(frame,
                                                     "Are you sure you want to remove profile '" + selectedProfile + "'?",
                                                     "Remove Profile",
                                                     JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                script.getConfig().removeProfile(selectedProfile);
                updateProfileSelector();
            }
        }
    }

    /**
     * Handles the close button click
     */
    private void handleClose() {
        context.setState(ScriptState.STOPPED);
    }

    /**
     * Disposes the GUI
     */
    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }

    /**
     * Checks if the GUI is visible
     */
    public boolean isVisible() {
        return frame != null && frame.isVisible();
    }

    // Helper methods for logging
    protected void debug(String message) {
        System.out.println("[DEBUG] [FrameworkGUI] " + message);
    }

    protected void error(String message, Exception e) {
        System.err.println("[ERROR] [FrameworkGUI] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
