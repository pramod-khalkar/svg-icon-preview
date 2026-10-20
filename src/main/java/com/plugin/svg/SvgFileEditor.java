package com.plugin.svg;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.OnePixelSplitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File editor for viewing and exporting SVG previews.
 */
public class SvgFileEditor implements FileEditor {
    private static final String NOTIFICATION_GROUP_ID = "SVG Toolkit";
    private final VirtualFile virtualFile;
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel contentPanel;
    private final CardLayout contentLayout;
    private final JPanel sourcePanel;
    private final JPanel previewContainer;
    private final JPanel editorOnlyPanel;
    private final JPanel previewOnlyPanel;
    private final OnePixelSplitter splitPane;
    private double splitRatio = 0.5;
    private final EditorEx svgEditor;
    private final SvgPreviewPanel previewPanel;
    private final JLabel imageDetailsLabel;
    private final Timer previewUpdateTimer;
    private final AtomicLong renderGeneration = new AtomicLong();
    private BufferedImage currentImage;
    private String svgText;
    private volatile boolean disposed;
    private static final Logger LOG = Logger.getInstance(SvgFileEditor.class);

    public SvgFileEditor(Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        Document document = Objects.requireNonNull(
                FileDocumentManager.getInstance().getDocument(virtualFile),
                "Unable to create a document for the SVG preview file");
        Editor editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, false);
        if (!(editor instanceof EditorEx)) {
            EditorFactory.getInstance().releaseEditor(editor);
            throw new IllegalStateException("IntelliJ did not create an extended SVG editor");
        }
        this.svgEditor = (EditorEx) editor;
        svgEditor.setVerticalScrollbarVisible(true);
        svgEditor.setHorizontalScrollbarVisible(true);
        svgEditor.getSettings().setLineNumbersShown(true);
        svgEditor.getSettings().setUseSoftWraps(false);
        this.previewPanel = new SvgPreviewPanel();
        this.imageDetailsLabel = new JLabel();
        this.imageDetailsLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.previewUpdateTimer = new Timer(300, event -> renderCurrentSvg());
        this.previewUpdateTimer.setRepeats(false);
        this.mainPanel = new JPanel(new BorderLayout(0, 0));

        this.sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder("SVG Source (editable)"));
        sourcePanel.add(svgEditor.getComponent(), BorderLayout.CENTER);
        JScrollPane previewScrollPane = new JScrollPane(previewPanel);
        this.previewContainer = new JPanel(new BorderLayout());
        previewContainer.add(createPreviewToolbar(), BorderLayout.NORTH);
        previewContainer.add(previewScrollPane, BorderLayout.CENTER);
        previewPanel.setViewport(previewScrollPane.getViewport());
        previewScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                previewPanel.updatePreferredSize();
            }
        });
        this.splitPane = new OnePixelSplitter(false, (float) splitRatio, 0.1f, 0.9f);
        splitPane.setFirstComponent(sourcePanel);
        splitPane.setSecondComponent(previewContainer);
        splitPane.setDividerWidth(8);
        splitPane.setResizeEnabled(true);
        splitPane.setShowDividerControls(false);
        splitPane.setShowDividerIcon(false);
        splitPane.setHonorComponentsMinimumSize(false);
        splitPane.addPropertyChangeListener(Splitter.PROP_PROPORTION,
                event -> splitRatio = splitPane.getProportion());
        this.editorOnlyPanel = new JPanel(new BorderLayout());
        this.previewOnlyPanel = new JPanel(new BorderLayout());
        this.contentLayout = new CardLayout();
        this.contentPanel = new JPanel(contentLayout);
        contentPanel.add(splitPane, "split");
        contentPanel.add(editorOnlyPanel, "editor");
        contentPanel.add(previewOnlyPanel, "preview");
        mainPanel.add(createLayoutToolbar(), BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        bindZoomShortcuts(mainPanel);

        loadContentFromVirtualFile();
        svgEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                renderGeneration.incrementAndGet();
                previewUpdateTimer.restart();
            }
        });
    }

    private void loadContentFromVirtualFile() {
        try {
            // Read content from the virtual file using its input stream
            byte[] bytes = virtualFile.contentsToByteArray();
            String rawContent = new String(bytes, StandardCharsets.UTF_8);

            // Decode the SVG content
            this.svgText = virtualFile instanceof SvgVirtualFile
                    ? ((SvgVirtualFile) virtualFile).getSvgText()
                    : null;
            if (this.svgText == null && !rawContent.isEmpty()) {
                try {
                    this.svgText = SvgDataUriUtil.decodeSvgContent(rawContent);
                } catch (Exception e) {
                    LOG.warn("[SVG Toolkit] Error decoding SVG: " + e.getMessage(), e);
                }
            }

            if (this.svgText != null) {
                BufferedImage previewImage = virtualFile instanceof SvgVirtualFile
                        ? ((SvgVirtualFile) virtualFile).getPreviewImage()
                        : null;
                this.currentImage = previewImage;
                this.previewPanel.setImage(previewImage);
                updateImageDetails(previewImage);
                if (previewImage == null) {
                    renderSvg(this.svgText);
                }
            } else {
                this.currentImage = null;
                this.previewPanel.setImage(null);
                imageDetailsLabel.setText("Invalid SVG - check idea.log for details");
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                LOG.warn("[SVG Toolkit] Error reading content from virtual file: " + e.getMessage(), e);
                this.currentImage = null;
                this.previewPanel.setImage(null);
                imageDetailsLabel.setText("Error reading file");
            } else {
                LOG.warn("[SVG Toolkit] Error processing SVG: " + e.getMessage(), e);
                this.currentImage = null;
                this.previewPanel.setImage(null);
                imageDetailsLabel.setText("Error processing SVG - see idea.log for details");
            }
        }
    }

    private void renderSvg(String source) {
        long generation = renderGeneration.get();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            BufferedImage image = SvgRenderer.render(source, 400, 400);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (disposed || generation != renderGeneration.get()) {
                    return;
                }
                currentImage = image;
                previewPanel.setImage(image);
                updateImageDetails(image);
            });
        });
    }

    private void renderCurrentSvg() {
        String source = svgEditor.getDocument().getText();
        long generation = renderGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String decoded = SvgDataUriUtil.decodeSvgContent(source);
            BufferedImage image = decoded == null ? null : SvgRenderer.render(decoded, 400, 400);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (disposed || generation != renderGeneration.get()) {
                    return;
                }
                svgText = decoded;
                currentImage = image;
                previewPanel.setImage(image);
                updateImageDetails(image);
            });
        });
    }

    private void updateImageDetails(@Nullable BufferedImage image) {
        if (image != null) {
            double kb = (image.getWidth() * image.getHeight() * 4.0) / 1024.0;
            imageDetailsLabel.setText(String.format("%d × %d px ~%.1fKB",
                    image.getWidth(), image.getHeight(), kb));
        } else {
            imageDetailsLabel.setText("Unable to render SVG");
        }
    }

    private JComponent createPreviewToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCCCCCC)));

        toolbar.add(createIconButton(AllIcons.Actions.Copy, "Copy as PNG", this::copyAsImageToClipboard));
        toolbar.add(createIconButton(AllIcons.Actions.Download, "Export PNG", this::exportAsPng));
        toolbar.add(createIconButton(AllIcons.Actions.Copy, "Copy Base64 data URI", this::copyBase64ToClipboard));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(createIconButton(AllIcons.General.ZoomOut, "Zoom out", previewPanel::zoomOut));
        toolbar.add(createIconButton(AllIcons.General.ZoomIn, "Zoom in", previewPanel::zoomIn));
        toolbar.add(createIconButton(AllIcons.General.ActualZoom, "Actual size (1:1)", previewPanel::showActualSize));
        toolbar.add(createIconButton(AllIcons.General.FitContent, "Fit image to preview", previewPanel::fitToViewport));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));

        JToggleButton checkerboardButton = createToggleButton(
                new PreviewOptionIcon(false), "Hide checkerboard background");
        checkerboardButton.setSelected(true);
        checkerboardButton.addActionListener(event -> {
            previewPanel.setCheckerboardVisible(checkerboardButton.isSelected());
            checkerboardButton.setToolTipText(checkerboardButton.isSelected()
                    ? "Hide checkerboard background"
                    : "Show checkerboard background");
        });
        toolbar.add(checkerboardButton);

        JToggleButton gridButton = createToggleButton(new PreviewOptionIcon(true), "Show grid");
        gridButton.addActionListener(event -> {
            previewPanel.setGridVisible(gridButton.isSelected());
            gridButton.setToolTipText(gridButton.isSelected() ? "Hide grid" : "Show grid");
        });
        toolbar.add(gridButton);
        toolbar.add(imageDetailsLabel);

        return toolbar;
    }

    private JComponent createLayoutToolbar() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        JToggleButton splitButton = createToggleButton(
                AllIcons.General.LayoutEditorPreview, "Show editor and preview");
        JToggleButton editorButton = createToggleButton(
                AllIcons.General.LayoutEditorOnly, "Show editor only");
        JToggleButton previewButton = createToggleButton(
                AllIcons.General.LayoutPreviewOnly, "Show preview only");
        ButtonGroup group = new ButtonGroup();
        group.add(splitButton);
        group.add(editorButton);
        group.add(previewButton);
        splitButton.setSelected(true);
        splitButton.addActionListener(event -> showLayout("split"));
        editorButton.addActionListener(event -> showLayout("editor"));
        previewButton.addActionListener(event -> showLayout("preview"));
        buttons.add(splitButton);
        buttons.add(editorButton);
        buttons.add(previewButton);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private void showLayout(String layout) {
        if ("split".equals(layout)) {
            splitPane.setFirstComponent(null);
            splitPane.setSecondComponent(null);
            splitPane.setFirstComponent(sourcePanel);
            splitPane.setSecondComponent(previewContainer);
            contentLayout.show(contentPanel, "split");
            splitPane.setProportion((float) Math.max(0.1, Math.min(0.9, splitRatio)));
        } else {
            splitPane.setFirstComponent(new JPanel());
            splitPane.setSecondComponent(new JPanel());
            if ("editor".equals(layout)) {
                editorOnlyPanel.add(sourcePanel, BorderLayout.CENTER);
                contentLayout.show(contentPanel, "editor");
            } else {
                previewOnlyPanel.add(previewContainer, BorderLayout.CENTER);
                contentLayout.show(contentPanel, "preview");
            }
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JButton createIconButton(Icon icon, String tooltip, Runnable action) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setPreferredSize(new Dimension(24, 24));
        button.setRolloverEnabled(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.getModel().addChangeListener(event -> updateButtonFeedback(button));
        button.addActionListener(event -> action.run());
        return button;
    }

    private JToggleButton createToggleButton(Icon icon, String tooltip) {
        JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setPreferredSize(new Dimension(24, 24));
        button.setRolloverEnabled(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.getModel().addChangeListener(event -> updateButtonFeedback(button));
        return button;
    }

    private void updateButtonFeedback(AbstractButton button) {
        ButtonModel model = button.getModel();
        boolean selected = model.isSelected();
        boolean active = selected || model.isPressed() || model.isRollover();
        Color activeColor = UIManager.getColor(selected ? "ToggleButton.select" : "Button.select");
        if (activeColor == null) {
            activeColor = new Color(0xDCE8F5);
        }
        Color idleColor = UIManager.getColor("Button.background");
        button.setBackground(active ? activeColor : idleColor);
        button.setBorderPainted(active || model.isRollover());
    }

    private void bindZoomShortcuts(JComponent target) {
        bindZoomShortcut(target, KeyEvent.VK_PLUS, "zoom-in", previewPanel::zoomIn);
        bindZoomShortcut(target, KeyEvent.VK_EQUALS, "zoom-in-equals", previewPanel::zoomIn);
        bindZoomShortcut(target, KeyEvent.VK_MINUS, "zoom-out", previewPanel::zoomOut);
        bindZoomShortcut(target, KeyEvent.VK_0, "actual-size", previewPanel::showActualSize);
    }

    private void bindZoomShortcut(JComponent target, int keyCode, String actionName, Runnable action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK);
        target.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionName);
        target.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    // FileEditor implementations
    @NotNull
    @Override
    public String getName() {
        String name = virtualFile.getName();
        // Remove the .svg-preview suffix for a cleaner tab title
        if (name.endsWith(".svg-preview")) {
            return name.substring(0, name.length() - ".svg-preview".length());
        }
        return name;
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
        return previewPanel;
    }

    @Override
    public void dispose() {
        disposed = true;
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
        private JViewport viewport;
        private double zoomLevel = 1.0;
        private boolean fitToViewport;
        private boolean checkerboardVisible = true;
        private boolean gridVisible;
        private static final int ZOOM_STEP = 10;
        private static final double MIN_ZOOM = 0.1;
        private static final double MAX_ZOOM = 4.0;
        private static final int GRID_SPACING = 10;
        private static final int CHECKER_SIZE = 12;

        SvgPreviewPanel() {
            setOpaque(true);
            setBackground(new Color(0xEEEEEE));
            setPreferredSize(new Dimension(300, 300));

            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    double rotation = e.getPreciseWheelRotation();
                    if (rotation == 0.0) {
                        rotation = e.getWheelRotation();
                    }
                    if (rotation < 0.0) {
                        zoomIn();
                    } else if (rotation > 0.0) {
                        zoomOut();
                    }
                } else {
                    scrollByWheel(e);
                }
                e.consume();
            });

        }

        private void scrollByWheel(MouseWheelEvent event) {
            if (viewport == null) {
                return;
            }

            double rotation = event.getPreciseWheelRotation();
            if (rotation == 0.0) {
                rotation = event.getWheelRotation();
            }
            int delta = (int) Math.round(rotation * 36.0);
            if (delta == 0 && rotation != 0.0) {
                delta = rotation > 0.0 ? 1 : -1;
            }

            Point current = viewport.getViewPosition();
            Dimension extent = viewport.getExtentSize();
            Dimension viewSize = viewport.getViewSize();
            if (event.isShiftDown()) {
                current.x += delta;
            } else {
                current.y += delta;
            }
            current.x = Math.max(0, Math.min(current.x, Math.max(0, viewSize.width - extent.width)));
            current.y = Math.max(0, Math.min(current.y, Math.max(0, viewSize.height - extent.height)));
            viewport.setViewPosition(current);
        }

        void setViewport(JViewport viewport) {
            this.viewport = viewport;
            updatePreferredSize();
        }

        void setImage(BufferedImage image) {
            this.image = image;
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void setCheckerboardVisible(boolean visible) {
            checkerboardVisible = visible;
            repaint();
        }

        void setGridVisible(boolean visible) {
            gridVisible = visible;
            repaint();
        }

        void zoomIn() {
            if (fitToViewport) {
                zoomLevel = getFitScale();
            }
            fitToViewport = false;
            zoomLevel = Math.min(zoomLevel + ZOOM_STEP / 100.0, MAX_ZOOM);
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void zoomOut() {
            if (fitToViewport) {
                zoomLevel = getFitScale();
            }
            fitToViewport = false;
            zoomLevel = Math.max(zoomLevel - ZOOM_STEP / 100.0, MIN_ZOOM);
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void showActualSize() {
            fitToViewport = false;
            zoomLevel = 1.0;
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void fitToViewport() {
            fitToViewport = true;
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void updatePreferredSize() {
            if (fitToViewport && viewport != null) {
                Dimension extent = viewport.getExtentSize();
                setPreferredSize(new Dimension(Math.max(1, extent.width), Math.max(1, extent.height)));
            } else if (image != null) {
                setPreferredSize(new Dimension(
                        (int) Math.ceil(image.getWidth() * zoomLevel),
                        (int) Math.ceil(image.getHeight() * zoomLevel)));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                if (checkerboardVisible) {
                    paintCheckerboard(g2d);
                } else {
                    Color themeBackground = UIManager.getColor("EditorPane.background");
                    if (themeBackground == null) {
                        themeBackground = UIManager.getColor("Panel.background");
                    }
                    if (themeBackground == null) {
                        themeBackground = getBackground();
                    }
                    g2d.setColor(themeBackground);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                if (image == null) {
                    return;
                }

                double scale = fitToViewport ? getFitScale() : zoomLevel;
                int scaledWidth = (int) Math.round(image.getWidth() * scale);
                int scaledHeight = (int) Math.round(image.getHeight() * scale);
                int x = (getWidth() - scaledWidth) / 2;
                int y = (getHeight() - scaledHeight) / 2;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.drawImage(image, x, y, scaledWidth, scaledHeight, this);

                if (gridVisible) {
                    paintGrid(g2d, x, y, scaledWidth, scaledHeight, scale);
                }
            } finally {
                g2d.dispose();
            }
        }

        private double getFitScale() {
            if (image == null || viewport == null) {
                return 1.0;
            }
            Dimension extent = viewport.getExtentSize();
            if (extent.width <= 0 || extent.height <= 0) {
                return 1.0;
            }
            return Math.min((double) extent.width / image.getWidth(),
                    (double) extent.height / image.getHeight());
        }

        private void paintCheckerboard(Graphics2D g2d) {
            Color light = new Color(0xF5F5F5);
            Color dark = new Color(0xD8D8D8);
            for (int y = 0; y < getHeight(); y += CHECKER_SIZE) {
                for (int x = 0; x < getWidth(); x += CHECKER_SIZE) {
                    g2d.setColor(((x / CHECKER_SIZE + y / CHECKER_SIZE) & 1) == 0 ? light : dark);
                    g2d.fillRect(x, y, CHECKER_SIZE, CHECKER_SIZE);
                }
            }
        }

        private void paintGrid(Graphics2D g2d, int x, int y, int width, int height, double scale) {
            if (scale <= 0.0) {
                return;
            }
            int spacing = Math.max(4, (int) Math.round(GRID_SPACING * scale));
            g2d.setColor(new Color(0x55000000, true));
            for (int gridX = x; gridX <= x + width; gridX += spacing) {
                g2d.drawLine(gridX, y, gridX, y + height);
            }
            for (int gridY = y; gridY <= y + height; gridY += spacing) {
                g2d.drawLine(x, gridY, x + width, gridY);
            }
        }
    }

    private static final class PreviewOptionIcon implements Icon {
        private final boolean grid;

        private PreviewOptionIcon(boolean grid) {
            this.grid = grid;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            try {
                Color foreground = component.getForeground();
                g2d.setColor(foreground == null ? Color.WHITE : foreground);
                if (grid) {
                    g2d.drawRect(x + 2, y + 2, 11, 11);
                    g2d.drawLine(x + 6, y + 2, x + 6, y + 13);
                    g2d.drawLine(x + 10, y + 2, x + 10, y + 13);
                    g2d.drawLine(x + 2, y + 6, x + 13, y + 6);
                    g2d.drawLine(x + 2, y + 10, x + 13, y + 10);
                } else {
                    int cell = 4;
                    for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 3; column++) {
                            if ((row + column) % 2 == 0) {
                                g2d.fillRect(x + 2 + column * cell, y + 2 + row * cell, cell, cell);
                            }
                        }
                    }
                    g2d.drawRect(x + 2, y + 2, 11, 11);
                }
            } finally {
                g2d.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    private void copyAsImageToClipboard() {
        if (currentImage == null) return;
        try {
            java.awt.datatransfer.Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            ImageSelection imageSelection = new ImageSelection(currentImage);
            clipboard.setContents(imageSelection, null);
            showCopyNotification("SVG image copied to clipboard as PNG.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Failed to copy: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyBase64ToClipboard() {
        try {
            if (svgText == null) {
                JOptionPane.showMessageDialog(mainPanel,
                        "The SVG source could not be decoded.",
                        "Invalid SVG", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String encodedDataUri = SvgDataUriUtil.encodeBase64DataUri(svgText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(encodedDataUri), null);
            showCopyNotification("Base64 SVG data URI copied to clipboard.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Failed to copy Base64: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCopyNotification(String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project);
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