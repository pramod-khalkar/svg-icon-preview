package com.plugin.svg;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Icon that can be updated with a rendered BufferedImage later.
 * Shows a simple placeholder until image is available.
 */
public class SvgDynamicIcon implements Icon {

    private static final Logger LOG = Logger.getInstance(SvgDynamicIcon.class);

    private final int size;
    private final AtomicReference<BufferedImage> imageRef = new AtomicReference<>();
    private final Project project; // used to trigger repaint/reindex safely

    public SvgDynamicIcon(int size, Project project) {
        this.size = size;
        this.project = project;
    }

    public boolean hasImage() {
        return imageRef.get() != null;
    }

    public void setImage(BufferedImage img) {
        imageRef.set(img);
        // Trigger UI refresh on EDT
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (project != null) {
                    DaemonCodeAnalyzer.getInstance(project).restart();
                }
            } catch (Exception e) {
                // Avoid spamming logs
                LOG.warn("[SVG Toolkit] Error while requesting UI refresh after icon update: " + e.getMessage());
            }
        });
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        BufferedImage img = imageRef.get();
        if (img != null) {
            g.drawImage(img, x, y, size, size, null);
            return;
        }
        // draw placeholder
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(new Color(0xE0E0E0));
            g2.fillRect(x, y, size, size);
            g2.setColor(new Color(0xA0A0A0));
            g2.drawRect(x, y, size - 1, size - 1);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, Math.max(8, size / 3f)));
            FontMetrics fm = g2.getFontMetrics();
            String s = "SVG";
            int tx = x + (size - fm.stringWidth(s)) / 2;
            int ty = y + (size + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(new Color(0x606060));
            g2.drawString(s, tx, ty);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
