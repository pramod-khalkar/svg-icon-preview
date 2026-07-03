package com.plugin.svg;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Settings UI for SVG Toolkit plugin.
 * Allows users to configure MAX_INLINE_SIZE and other preferences.
 */
public class SvgToolkitSettingsConfigurable implements Configurable {
    private SvgToolkitSettingsPanel settingsPanel;
    private final SvgToolkitSettings settings;

    public SvgToolkitSettingsConfigurable() {
        this.settings = ServiceManager.getService(SvgToolkitSettings.class);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "SVG Toolkit";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "preferences.svgtoolkit";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new SvgToolkitSettingsPanel(settings);
        }
        return settingsPanel.getRoot();
    }

    @Override
    public boolean isModified() {
        return settingsPanel != null && settingsPanel.isModified(settings);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null) {
            settingsPanel.apply(settings);
        }
    }

    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.reset(settings);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}
