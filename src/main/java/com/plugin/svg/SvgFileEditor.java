package com.plugin.svg;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.ui.JBUI;
import com.plugin.svg.SvgDataUriUtil;
import com.plugin.svg.SvgRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File editor for SVG previews showing Base64 source and visual preview side by side.
 */
public class SvgFileEditor implements FileEditor {
    // Broad data URI pattern: capture optional base64 marker and payload (DOTALL to allow newlines)
    private static final Pattern SVG_ICON_PATTERN = Pattern.compile("(?i)data:image/svg\\+xml(?:;charset=[^,;]+)?(?:;(base64))?,(.*)", Pattern.DOTALL);
    private final Project project;
    private final VirtualFile virtualFile;
    private final JPanel mainPanel;
    private final JTextArea base64TextArea;
    private final SvgPreviewPanel previewPanel;
    private final JLabel imageDetailsLabel;
    private BufferedImage currentImage;
    private String currentBase64;
    private static final Logger LOG = Logger.getInstance(SvgFileEditor.class);

    public SvgFileEditor(Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;

        // Initialize components
        this.base64TextArea = new JTextArea();
        this.base64TextArea.setEditable(false);
        this.base64TextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.base64TextArea.setLineWrap(true);
        this.base64TextArea.setWrapStyleWord(true);

        this.previewPanel = new SvgPreviewPanel();
        this.imageDetailsLabel = new JLabel();
        this.imageDetailsLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Main panel with toolbar on top and split pane below
        this.mainPanel = new JPanel(new BorderLayout(0, 0));

        // Toolbar
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Split pane for base64 and preview
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(base64TextArea),
                new JScrollPane(previewPanel));
        splitPane.setResizeWeight(0.5); // 50/50 split
        splitPane.setOneTouchExpandable(true);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Load content from the virtual file
        loadContentFromVirtualFile();
    }

    private void loadContentFromVirtualFile() {
        try {
            // Read content from the virtual file using its input stream
            byte[] bytes = virtualFile.contentsToByteArray();
            String rawContent = new String(bytes, StandardCharsets.UTF_8);
            this.currentBase64 = rawContent;

            // Decode the SVG content
            String svgText = null;
            if (rawContent != null && !rawContent.isEmpty()) {
                try {
                    // Extract payload and marker from the data URI string
                    Matcher m = SVG_ICON_PATTERN.matcher(rawContent);
                    if (m.matches()) {
                        String marker = m.group(1); // 'base64' if present
                        String payload = m.group(2);
                        if (payload != null) {
                            payload = SvgDataUriUtil.normalizePayload(payload);
                            try {
                                svgText = SvgDataUriUtil.decodePayload(payload, marker != null);
                            } catch (Exception e) {
                                LOG.warn("[SVG Toolkit] Error decoding SVG: " + e.getMessage(), e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("[SVG Toolkit] Error decoding SVG: " + e.getMessage(), e);
                }
            }

            // Show decoded SVG in the text area (or raw content if decoding failed)
            this.base64TextArea.setText(svgText != null ? svgText : rawContent);

            // Render SVG preview from the decoded content
            if (svgText != null) {
                BufferedImage previewImage = SvgRenderer.render(svgText, 400, 400); // Default size
                this.currentImage = previewImage;
                this.previewPanel.setImage(previewImage);

                // Update image details
                if (previewImage != null) {
                    // Calculate approximate size in KB (assuming 4 bytes per pixel for ARGB)
                    double kb = (previewImage.getWidth() * previewImage.getHeight() * 4.0) / 1024.0;
                    imageDetailsLabel.setText(String.format("%d × %d px ~%.1fKB",
                            previewImage.getWidth(), previewImage.getHeight(), kb));
                } else {
                    imageDetailsLabel.setText("No image");
                }
            } else {
                this.currentImage = null;
                this.previewPanel.setImage(null);
                imageDetailsLabel.setText("Invalid SVG");
            }
        } catch (IOException e) {
            LOG.warn("[SVG Toolkit] Error reading content from virtual file: " + e.getMessage(), e);
            this.base64TextArea.setText("Error reading file content");
            this.currentImage = null;
            this.previewPanel.setImage(null);
            imageDetailsLabel.setText("Error reading file");
        }
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

        JButton copyButton = new JButton("Copy as PNG");
        copyButton.addActionListener(e -> copyAsImageToClipboard());
        toolbar.add(copyButton);

        JButton exportButton = new JButton("Export PNG");
        exportButton.addActionListener(e -> exportAsPng());
        toolbar.add(exportButton);

        toolbar.add(imageDetailsLabel);

        return toolbar;
    }

    // FileEditor implementations
    @NotNull
    @Override
    public String getName() {
        return "SVG Preview";
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return false; // This is just a preview, not editable
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
        // Called when editor gains focus
    }

    @Override
    public void deselectNotify() {
        // Called when editor loses focus
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Not needed for this simple editor
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Not needed
    }

    @Override
    public @Nullable FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return null; // No state to save
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // Not needed
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return base64TextArea;
    }

    @Override
    public void dispose() {
        // Cleanup resources if needed
    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        // Not needed
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return virtualFile;
    }

    // Preview panel class for displaying and zooming SVG images
    private class SvgPreviewPanel extends JPanel {
        private BufferedImage image;
        private int zoomLevel = 100; // 100%
        private static final int ZOOM_STEP = 10;
        private static final int MIN_ZOOM = 10;
        private static final int MAX_ZOOM = 400;

        SvgPreviewPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(300, 300));

            // Add mouse wheel for zoom
            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    int notches = e.getWheelRotation();
                    if (notches < 0) {
                        zoomIn();
                    } else if (notches > 0) {
                        zoomOut();
                    }
                    e.consume();
                }
            });

            // Add keyboard shortcuts
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(ke -> {
                        if (ke.getID() == KeyEvent.KEY_PRESSED && isVisible()) {
                            if (ke.isControlDown()) {
                                if (ke.getKeyCode() == KeyEvent.VK_PLUS ||
                                    ke.getKeyCode() == KeyEvent.VK_EQUALS) {
                                    zoomIn();
                                    return true;
                                } else if (ke.getKeyCode() == KeyEvent.VK_MINUS) {
                                    zoomOut();
                                    return true;
                                } else if (ke.getKeyCode() == KeyEvent.VK_0) {
                                    resetZoom();
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
        }

        void setImage(BufferedImage image) {
            this.image = image;
            zoomLevel = 100;
            updatePreferredSize();
            revalidate();
            repaint();
        }

        private void zoomIn() {
            if (zoomLevel < MAX_ZOOM) {
                zoomLevel = Math.min(zoomLevel + ZOOM_STEP, MAX_ZOOM);
                updatePreferredSize();
                revalidate();
                repaint();
            }
        }

        private void zoomOut() {
            if (zoomLevel > MIN_ZOOM) {
                zoomLevel = Math.max(zoomLevel - ZOOM_STEP, MIN_ZOOM);
                updatePreferredSize();
                revalidate();
                repaint();
            }
        }

        private void resetZoom() {
            zoomLevel = 100;
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
                int x = (getWidth() - scaledWidth) / 2;
                int y = (getHeight() - scaledHeight) / 2;
                g2d.drawImage(image, x, y, scaledWidth, scaledHeight, this);
            }
        }
    }

    private void copyAsImageToClipboard() {
        if (currentImage == null) return;
        try {
            java.awt.datatransfer.Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            ImageSelection imageSelection = new ImageSelection(currentImage);
            clipboard.setContents(imageSelection, null);
            JOptionPane.showMessageDialog(mainPanel,
                    "SVG image copied to clipboard as PNG",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Failed to copy: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAsPng() {
        if (currentImage == null) return;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export SVG as PNG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Image (*.png)", "png"));

        if (fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(mainPanel)) ==
                JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                if (!filePath.endsWith(".png")) {
                    filePath += ".png";
                }
                boolean success = ImageIO.write(currentImage, "PNG", new File(filePath));
                if (success) {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(mainPanel),
                            "Exported to: " + filePath,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(mainPanel),
                            "Failed to write PNG file",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(mainPanel),
                        "Export failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Helper class for clipboard image transfer (same as before)
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