package com.plugin.svg;

import javax.swing.*;

/**
 * UI Panel for SVG Toolkit settings.
 * Provides controls for configuring MAX_INLINE_SIZE.
 */
public class SvgToolkitSettingsPanel {
    private JPanel root;
    private JSpinner maxInlineSizeSpinner;
    private JLabel maxInlineSizeLabel;
    private JLabel infoLabel;

    public SvgToolkitSettingsPanel(SvgToolkitSettings settings) {
        createUI();
        reset(settings);
    }

    private void createUI() {
        root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // Add spacing
        root.add(Box.createVerticalStrut(10));

        // Max Inline Size setting
        JPanel inlineSizePanel = new JPanel();
        inlineSizePanel.setLayout(new BoxLayout(inlineSizePanel, BoxLayout.X_AXIS));

        maxInlineSizeLabel = new JLabel("Maximum inline SVG size (KB):");
        maxInlineSizeSpinner = new JSpinner(
            new SpinnerNumberModel(
                SvgToolkitSettings.DEFAULT_MAX_INLINE_SIZE_KB,
                10,     // min: 10 KB
                10000,  // max: 10 MB
                50      // step: 50 KB
            )
        );
        maxInlineSizeSpinner.setPreferredSize(new java.awt.Dimension(100, 25));

        inlineSizePanel.add(maxInlineSizeLabel);
        inlineSizePanel.add(Box.createHorizontalStrut(10));
        inlineSizePanel.add(maxInlineSizeSpinner);
        inlineSizePanel.add(Box.createHorizontalGlue());

        root.add(inlineSizePanel);
        root.add(Box.createVerticalStrut(10));

        // Info label
        infoLabel = new JLabel("<html>" +
            "SVGs smaller than or equal to this size will render immediately.<br>" +
            "Larger SVGs will show a placeholder and render on demand to conserve memory." +
            "</html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(infoLabel.getFont().getSize() - 2f));
        root.add(infoLabel);
        root.add(Box.createVerticalGlue());
    }

    public JComponent getRoot() {
        return root;
    }

    public void reset(SvgToolkitSettings settings) {
        maxInlineSizeSpinner.setValue(settings.maxInlineSizeKb);
    }

    public boolean isModified(SvgToolkitSettings settings) {
        return ((int) maxInlineSizeSpinner.getValue()) != settings.maxInlineSizeKb;
    }

    public void apply(SvgToolkitSettings settings) {
        settings.maxInlineSizeKb = (int) maxInlineSizeSpinner.getValue();
    }
}
