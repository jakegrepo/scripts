package scripts.AIO.gui;

import core.context.ScriptContext;
import core.gui.AbstractSettingsTab;
import org.dreambot.api.methods.skills.Skill;
import scripts.AIO.AIOConfig;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Simple settings tab allowing goal levels and excluded methods per skill.
 */
public class AIOSettingsTab extends AbstractSettingsTab {
    private final AIOConfig config;
    private final Map<Skill, JSpinner> levelSpinners = new EnumMap<>(Skill.class);
    private final Map<Skill, JTextField> excludeFields = new EnumMap<>(Skill.class);

    public AIOSettingsTab(ScriptContext context) {
        super(context);
        this.config = (AIOConfig) context.getScript().getContext().config().getConfig("aio_config");
    }

    @Override
    public void createComponents() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        Skill[] skills = {Skill.FISHING, Skill.WOODCUTTING, Skill.COOKING, Skill.FLETCHING};
        for (Skill skill : skills) {
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel(skill.getName() + " Goal"), gbc);
            gbc.gridx = 1;
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(config.getGoalLevel(skill), 1, 99, 1));
            levelSpinners.put(skill, spinner);
            panel.add(spinner, gbc);

            gbc.gridx = 2;
            panel.add(new JLabel("Exclude"), gbc);
            gbc.gridx = 3;
            JTextField field = new JTextField(config.getExcludedMethods(skill));
            excludeFields.put(skill, field);
            panel.add(field, gbc);
            row++;
        }

        getPanel().setLayout(new BorderLayout());
        getPanel().add(panel, BorderLayout.NORTH);
    }

    @Override
    public void saveSettings() {
        for (Map.Entry<Skill, JSpinner> entry : levelSpinners.entrySet()) {
            config.setGoalLevel(entry.getKey(), (int) entry.getValue().getValue());
        }
        for (Map.Entry<Skill, JTextField> entry : excludeFields.entrySet()) {
            config.setExcludedMethods(entry.getKey(), entry.getValue().getText());
        }
        config.saveConfig();
    }

    @Override
    public void resetToDefaults() {
        config.setDefaults();
        updateFromConfig();
    }

    @Override
    public void updateFromConfig() {
        for (Skill skill : levelSpinners.keySet()) {
            levelSpinners.get(skill).setValue(config.getGoalLevel(skill));
            excludeFields.get(skill).setText(config.getExcludedMethods(skill));
        }
    }

    @Override
    public String getTitle() {
        return "AIO";
    }
}
