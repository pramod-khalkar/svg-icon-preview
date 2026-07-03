package com.plugin.svg;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Enhanced SVG preview dialog with zoom controls and copy-as-PNG action.
 */
public class SvgPreviewDialog extends JDialog {
    private static final int ZOOM_STEP = 10; // 10% per zoom click
    private static final int MIN_ZOOM = 10; // 10%
    private static final int MAX_ZOOM = 400; // 400%
    private static final int DEFAULT_ZOOM = 100; // 100%

    private final BufferedImage originalImage;
    private final ZoomableImagePanel imagePanel;
    private int zoomLevel = DEFAULT_ZOOM;

    public SvgPreviewDialog(Frame parent, BufferedImage image) {
        super(parent, "SVG Preview", false);
        this.originalImage = image;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top toolbar with zoom and export buttons
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        // Central image panel with zoom support
        imagePanel = new ZoomableImagePanel(originalImage);
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);

        // Size and show
        setSize(600, 500);
        setLocationRelativeTo(null);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setBorder(BorderFactory.createEtchedBorder());

        // Zoom out button
        JButton zoomOutBtn = new JButton("-");
        zoomOutBtn.setToolTipText("Zoom out (Ctrl+Minus)");
        zoomOutBtn.addActionListener(e -> zoomOut());
        toolbar.add(zoomOutBtn);

        // Zoom level label
        JLabel zoomLabel = new JLabel(DEFAULT_ZOOM + "%");
        zoomLabel.setPreferredSize(new Dimension(50, 25));
        toolbar.add(zoomLabel);

        // Zoom in button
        JButton zoomInBtn = new JButton("+");
        zoomInBtn.setToolTipText("Zoom in (Ctrl+Plus)");
        zoomInBtn.addActionListener(e -> {
            zoomIn();
            zoomLabel.setText(zoomLevel + "%");
        });
        toolbar.add(zoomInBtn);

        // Reset zoom
        JButton resetBtn = new JButton("Reset");
        resetBtn.setToolTipText("Reset to 100% zoom");
        resetBtn.addActionListener(e -> {
            resetZoom();
            zoomLabel.setText(zoomLevel + "%");
        });
        toolbar.add(resetBtn);

        // Add separator
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(5, 30));
        toolbar.add(separator);

        // Copy as PNG button
        JButton copyPngBtn = new JButton("Copy as PNG");
        copyPngBtn.setToolTipText("Copy SVG as PNG to clipboard");
        copyPngBtn.addActionListener(e -> copyAsImageToClipboard());
        toolbar.add(copyPngBtn);

        // Export as PNG button
        JButton exportPngBtn = new JButton("Export PNG");
        exportPngBtn.setToolTipText("Save SVG as PNG file");
        exportPngBtn.addActionListener(e -> exportAsPng());
        toolbar.add(exportPngBtn);

        // Update zoom label on button click
        zoomOutBtn.addActionListener(e -> zoomLabel.setText(zoomLevel + "%"));

        // Keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isVisible()) {
                if (e.isControlDown()) {
                    if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                        zoomIn();
                        zoomLabel.setText(zoomLevel + "%");
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_MINUS) {
                        zoomOut();
                        zoomLabel.setText(zoomLevel + "%");
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_0) {
                        resetZoom();
                        zoomLabel.setText(zoomLevel + "%");
                        return true;
                    }
                }
            }
            return false;
        });

        return toolbar;
    }

    private void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(zoomLevel + ZOOM_STEP, MAX_ZOOM);
            imagePanel.setZoom(zoomLevel);
        }
    }

    private void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(zoomLevel - ZOOM_STEP, MIN_ZOOM);
            imagePanel.setZoom(zoomLevel);
        }
    }

    private void resetZoom() {
        zoomLevel = DEFAULT_ZOOM;
        imagePanel.setZoom(zoomLevel);
    }

    private void copyAsImageToClipboard() {
        try {
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            ImageSelection imageSelection = new ImageSelection(originalImage);
            clipboard.setContents(imageSelection, null);
            JOptionPane.showMessageDialog(this, "SVG image copied to clipboard as PNG",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to copy: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAsPng() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export SVG as PNG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "PNG Image (*.png)", "png"
        ));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                if (!filePath.endsWith(".png")) {
                    filePath += ".png";
                }
                boolean success = ImageIO.write(originalImage, "PNG", new File(filePath));
                if (success) {
                    JOptionPane.showMessageDialog(this, "Exported to: " + filePath,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to write PNG file",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Panel that displays image with zoom support.
     */
    private static class ZoomableImagePanel extends JPanel {
        private final BufferedImage image;
        private int zoomLevel = 100;

        ZoomableImagePanel(BufferedImage image) {
            this.image = image;
            setBackground(Color.WHITE);
        }

        void setZoom(int percent) {
            this.zoomLevel = percent;
            updatePreferredSize();
            revalidate();
            repaint();
        }

        private void updatePreferredSize() {
            if (image != null) {
                int scaledWidth = (int) (image.getWidth() * zoomLevel / 100.0);
                int scaledHeight = (int) (image.getHeight() * zoomLevel / 100.0);
                setPreferredSize(new Dimension(scaledWidth, scaledHeight));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                int scaledWidth = (int) (image.getWidth() * zoomLevel / 100.0);
                int scaledHeight = (int) (image.getHeight() * zoomLevel / 100.0);
                g2d.drawImage(image, 0, 0, scaledWidth, scaledHeight, this);
            }
        }
    }

    /**
     * Helper class for clipboard image transfer.
     */
    private static class ImageSelection implements java.awt.datatransfer.Transferable {
        private final BufferedImage image;

        ImageSelection(BufferedImage image) {
            this.image = image;
        }

        @Override
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            return java.awt.datatransfer.DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) {
            return image;
        }
    }
}
