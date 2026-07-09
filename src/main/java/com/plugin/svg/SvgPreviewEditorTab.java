package com.plugin.svg;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * IDE editor tab for SVG preview.
 * Layout:
 *   TOP    — toolbar (Copy PNG, Export PNG, zoom controls, image info)
 *   CENTER — JSplitPane: left = base64 raw text | right = rendered image
 *
 * @author Pramod Khalkar
 */
public class SvgPreviewEditorTab implements FileEditor {

    private static final int ZOOM_STEP = 10;
    private static final int MIN_ZOOM  = 10;
    private static final int MAX_ZOOM  = 400;
    private static final int DEFAULT_ZOOM = 100;

    private final SvgVirtualFile svgFile;
    private final JPanel rootPanel;
    private final ZoomableImagePanel imagePanel;
    private BufferedImage originalImage;
    private int zoomLevel = DEFAULT_ZOOM;

    // toolbar widgets
    private JLabel zoomLabel;
    private JLabel infoLabel;

    public SvgPreviewEditorTab(@NotNull SvgVirtualFile svgFile) {
        this.svgFile = svgFile;
        this.originalImage = svgFile.getPreviewImage();

        rootPanel = new JPanel(new BorderLayout());

        // ── Toolbar ──────────────────────────────────────────────────────
        rootPanel.add(buildToolbar(), BorderLayout.NORTH);

        // ── Split pane ───────────────────────────────────────────────────
        // Left: raw base64 / SVG text in a read-only text area
        JTextArea textArea = new JTextArea(svgFile.getRawBase64());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(new Color(43, 43, 43));
        textArea.setForeground(new Color(169, 183, 198));
        textArea.setCaretColor(Color.WHITE);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setBorder(BorderFactory.createTitledBorder(
                new MatteBorder(0, 0, 0, 1, new Color(60, 63, 65)),
                "Base64 Source",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 11),
                new Color(169, 183, 198)));

        // Right: rendered image
        imagePanel = new ZoomableImagePanel(originalImage);
        JScrollPane imageScroll = new JScrollPane(imagePanel);
        imageScroll.getViewport().setBackground(new Color(60, 63, 65));
        imageScroll.setBorder(BorderFactory.createTitledBorder(
                null,
                "Preview",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 11),
                new Color(169, 183, 198)));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScroll, imageScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setContinuousLayout(true);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        updateInfoLabel();
        bindKeyboardShortcuts();
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBackground(new Color(49, 51, 53));
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(30, 30, 30)));

        // Copy PNG
        toolbar.add(styledButton("Copy as PNG", "Copy rendered image to clipboard", e -> copyToClipboard()));

        // Export PNG
        toolbar.add(styledButton("Export PNG", "Save rendered image as a .png file", e -> exportPng()));

        toolbar.add(separator());

        // Zoom controls
        JButton zoomOut = styledButton("−", "Zoom out (Ctrl+Minus)", e -> { adjustZoom(-ZOOM_STEP); });
        toolbar.add(zoomOut);

        zoomLabel = new JLabel(DEFAULT_ZOOM + "%");
        zoomLabel.setForeground(Color.WHITE);
        zoomLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        zoomLabel.setPreferredSize(new Dimension(48, 24));
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        toolbar.add(zoomLabel);

        JButton zoomIn = styledButton("+", "Zoom in (Ctrl+Plus)", e -> { adjustZoom(ZOOM_STEP); });
        toolbar.add(zoomIn);

        JButton reset = styledButton("1:1", "Reset zoom to 100%", e -> { resetZoom(); });
        toolbar.add(reset);

        toolbar.add(separator());

        // Image info
        infoLabel = new JLabel();
        infoLabel.setForeground(new Color(169, 183, 198));
        infoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        toolbar.add(infoLabel);

        return toolbar;
    }

    private static JButton styledButton(String text, String tooltip, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(69, 73, 74));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(85, 85, 85)),
                new EmptyBorder(3, 8, 3, 8)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private static JSeparator separator() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(new Color(85, 85, 85));
        return sep;
    }

    // ── Zoom ─────────────────────────────────────────────────────────────────

    private void adjustZoom(int delta) {
        int next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel + delta));
        if (next != zoomLevel) {
            zoomLevel = next;
            imagePanel.setZoom(zoomLevel);
            zoomLabel.setText(zoomLevel + "%");
        }
    }

    private void resetZoom() {
        zoomLevel = DEFAULT_ZOOM;
        imagePanel.setZoom(zoomLevel);
        zoomLabel.setText(zoomLevel + "%");
    }

    private void bindKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && rootPanel.isShowing() && e.isControlDown()) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_PLUS || code == KeyEvent.VK_EQUALS) { adjustZoom(ZOOM_STEP);  return true; }
                if (code == KeyEvent.VK_MINUS)                               { adjustZoom(-ZOOM_STEP); return true; }
                if (code == KeyEvent.VK_0)                                   { resetZoom();            return true; }
            }
            return false;
        });
    }

    // ── Info label ───────────────────────────────────────────────────────────

    private void updateInfoLabel() {
        if (infoLabel == null) return;
        if (originalImage != null) {
            int w = originalImage.getWidth();
            int h = originalImage.getHeight();
            double kb = svgFile.getSvgText().length() / 1024.0;
            DecimalFormat df = new DecimalFormat("0.##");
            infoLabel.setText(String.format("  %d × %d px  |  SVG %s KB", w, h, df.format(kb)));
        } else {
            infoLabel.setText("  No image");
        }
    }

    // ── Clipboard ────────────────────────────────────────────────────────────

    private void copyToClipboard() {
        if (originalImage == null) return;
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new ImageTransferable(originalImage), null);
            JOptionPane.showMessageDialog(rootPanel, "Image copied to clipboard as PNG.",
                    "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(rootPanel, "Copy failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Export ───────────────────────────────────────────────────────────────

    private void exportPng() {
        if (originalImage == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export SVG as PNG");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image (*.png)", "png"));
        if (chooser.showSaveDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".png")) path += ".png";
                boolean ok = ImageIO.write(originalImage, "PNG", new File(path));
                if (ok) JOptionPane.showMessageDialog(rootPanel, "Exported to: " + path, "Success", JOptionPane.INFORMATION_MESSAGE);
                else    JOptionPane.showMessageDialog(rootPanel, "Failed to write file.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(rootPanel, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── FileEditor contract ───────────────────────────────────────────────────

    @Override public @NotNull JComponent getComponent() { return rootPanel; }
    @Override public @Nullable JComponent getPreferredFocusedComponent() { return rootPanel; }
    @Override public @NotNull String getName() { return "SVG Preview"; }
    @Override public boolean isModified() { return false; }
    @Override public boolean isValid() { return true; }
    @Override public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void dispose() {}
    @Override
    public void setState(@NotNull FileEditorState state) {
        // Not needed
    }
    @Override public <T> @Nullable T getUserData(@NotNull Key<T> key) { return null; }
    @Override public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {}

    // ── Inner: zoomable image panel ───────────────────────────────────────────

    private static class ZoomableImagePanel extends JPanel {
        private BufferedImage image;
        private int zoom = 100;

        ZoomableImagePanel(BufferedImage img) {
            this.image = img;
            setBackground(new Color(60, 63, 65));
            updateSize();
        }

        void setImage(BufferedImage img) {
            this.image = img;
            updateSize();
            repaint();
        }

        void setZoom(int pct) {
            this.zoom = pct;
            updateSize();
            repaint();
        }

        private void updateSize() {
            if (image != null) {
                int w = (int)(image.getWidth()  * zoom / 100.0);
                int h = (int)(image.getHeight() * zoom / 100.0);
                setPreferredSize(new Dimension(w, h));
            }
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = (int)(image.getWidth()  * zoom / 100.0);
            int h = (int)(image.getHeight() * zoom / 100.0);
            // Centre in viewport
            int x = Math.max(0, (getWidth()  - w) / 2);
            int y = Math.max(0, (getHeight() - h) / 2);
            g2.drawImage(image, x, y, w, h, this);
        }
    }

    // ── Inner: clipboard transferable ────────────────────────────────────────

    private static class ImageTransferable implements Transferable {
        private final BufferedImage image;
        ImageTransferable(BufferedImage img) { this.image = img; }
        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
        @Override public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.imageFlavor.equals(f); }
        @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(f)) throw new UnsupportedFlavorException(f);
            return image;
        }
    }
}
