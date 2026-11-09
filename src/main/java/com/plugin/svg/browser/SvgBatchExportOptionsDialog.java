package com.plugin.svg.browser;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

final class SvgBatchExportOptionsDialog {
    private static final int[] PRESET_SIZES = {16, 24, 32, 48, 64, 128};

    private SvgBatchExportOptionsDialog() {
    }

    static @Nullable List<Integer> show(Component parent) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Choose maximum edge sizes (aspect ratio is preserved):"),
                BorderLayout.NORTH);
        JPanel presetsPanel = new JPanel(new GridLayout(0, 3, 8, 2));
        List<JCheckBox> presetCheckBoxes = new ArrayList<>();
        for (int size : PRESET_SIZES) {
            JCheckBox checkBox = new JCheckBox(size + " px", size == 24 || size == 48 || size == 128);
            presetCheckBoxes.add(checkBox);
            presetsPanel.add(checkBox);
        }
        panel.add(presetsPanel, BorderLayout.CENTER);

        JPanel customPanel = new JPanel(new BorderLayout(0, 4));
        customPanel.add(new JLabel("Additional maximum edge sizes (1–"
                + SvgBatchPngExporter.MAX_DIMENSION + " px), e.g. 256, 512:"), BorderLayout.NORTH);
        JTextField customSizesField = new JTextField(32);
        customPanel.add(customSizesField, BorderLayout.CENTER);
        panel.add(customPanel, BorderLayout.SOUTH);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(parent, panel, "Export PNGs",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }

            TreeSet<Integer> sizes = new TreeSet<>();
            for (int index = 0; index < PRESET_SIZES.length; index++) {
                if (presetCheckBoxes.get(index).isSelected()) {
                    sizes.add(PRESET_SIZES[index]);
                }
            }
            try {
                sizes.addAll(SvgBatchPngExporter.parseCustomSizes(customSizesField.getText()));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(parent, exception.getMessage(),
                        "Invalid export size", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (sizes.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Choose at least one output size.",
                        "No output size selected", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            return List.copyOf(sizes);
        }
    }
}
