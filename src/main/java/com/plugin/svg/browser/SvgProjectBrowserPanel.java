package com.plugin.svg.browser;

import com.plugin.svg.action.OpenSvgWithToolkitAction;
import com.plugin.svg.data.SvgDataUriUtil;
import com.plugin.svg.editor.SvgDimensions;
import com.plugin.svg.editor.SvgVirtualFile;
import com.plugin.svg.render.SvgRenderer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

final class SvgProjectBrowserPanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(SvgProjectBrowserPanel.class);
    private static final int THUMBNAIL_SIZE = 64;

    private final Project project;
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
    private final JList<VirtualFile> fileList = new JList<>(listModel);
    private final JTextField embeddedSearchField = new JTextField();
    private final JLabel embeddedStatusLabel = new JLabel(" ");
    private final DefaultListModel<SvgEmbeddedSearch.Match> embeddedListModel = new DefaultListModel<>();
    private final JList<SvgEmbeddedSearch.Match> embeddedList = new JList<>(embeddedListModel);
    private final List<VirtualFile> projectSvgFiles = new ArrayList<>();
    private final List<SvgEmbeddedSearch.Match> embeddedMatches = new ArrayList<>();
    private final AtomicLong scanGeneration = new AtomicLong();
    private final AtomicLong embeddedSearchGeneration = new AtomicLong();
    private int unreadableEmbeddedFiles;
    private final SvgThumbnailCache thumbnailCache = new SvgThumbnailCache();
    private volatile boolean disposed;

    SvgProjectBrowserPanel(Project project) {
        super(new BorderLayout(0, 6));
        this.project = project;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        searchField.setToolTipText("Filter project SVG files by name or folder");
        fileList.setCellRenderer(new SvgFileCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFixedCellHeight(82);
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    openSelectedFile();
                }
            }
        });
        fileList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-selected-svg");
        fileList.getActionMap().put("open-selected-svg", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openSelectedFile();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilter();
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("SVG Files", createSvgFilesTab());
        tabs.addTab("SVG Data URIs", createEmbeddedSvgTab());
        add(tabs, BorderLayout.CENTER);
    }

    void refresh() {
        refreshSvgFiles();
        refreshEmbeddedSvgs();
    }

    private JPanel createSvgFilesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);
        JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
        refreshButton.setToolTipText("Refresh project SVG files");
        refreshButton.addActionListener(event -> refreshSvgFiles());
        searchPanel.add(refreshButton, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(searchPanel, BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEmbeddedSvgTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        embeddedSearchField.setToolTipText("Filter embedded SVG matches by file, line, or source");
        JPanel controls = new JPanel(new BorderLayout(6, 0));
        controls.add(embeddedSearchField, BorderLayout.CENTER);
        JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
        refreshButton.setToolTipText("Search project files for embedded SVG data URIs");
        refreshButton.addActionListener(event -> refreshEmbeddedSvgs());
        controls.add(refreshButton, BorderLayout.EAST);

        embeddedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        embeddedList.setCellRenderer(new EmbeddedSvgCellRenderer());
        embeddedList.setFixedCellHeight(82);
        embeddedList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    openSelectedEmbeddedPreview();
                }
            }
        });
        embeddedList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "preview-embedded-svg");
        embeddedList.getActionMap().put("preview-embedded-svg", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openSelectedEmbeddedPreview();
            }
        });

        JButton previewButton = new JButton("Preview");
        previewButton.addActionListener(event -> openSelectedEmbeddedPreview());
        JButton openSourceButton = new JButton("Open Source");
        openSourceButton.addActionListener(event -> openSelectedEmbeddedSource());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 0));
        actions.add(previewButton);
        actions.add(openSourceButton);
        embeddedList.addListSelectionListener(event -> {
            boolean hasSelection = embeddedList.getSelectedValue() != null;
            previewButton.setEnabled(hasSelection);
            openSourceButton.setEnabled(hasSelection);
        });
        previewButton.setEnabled(false);
        openSourceButton.setEnabled(false);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(controls, BorderLayout.NORTH);
        header.add(embeddedStatusLabel, BorderLayout.CENTER);
        header.add(actions, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(embeddedList), BorderLayout.CENTER);

        embeddedSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyEmbeddedFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyEmbeddedFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyEmbeddedFilter();
            }
        });
        return panel;
    }

    private void refreshSvgFiles() {
        long generation = scanGeneration.incrementAndGet();
        statusLabel.setText("Searching project for SVG files…");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Searching for SVG files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                List<VirtualFile> files = ProjectSvgFileScanner.scan(project, indicator);
                indicator.checkCanceled();
                SwingUtilities.invokeLater(() -> {
                    if (!disposed && !project.isDisposed() && generation == scanGeneration.get()) {
                        projectSvgFiles.clear();
                        projectSvgFiles.addAll(files);
                        applyFilter();
                    }
                });
            }
        });
    }

    private void refreshEmbeddedSvgs() {
        long generation = embeddedSearchGeneration.incrementAndGet();
        embeddedStatusLabel.setText("Searching project files for embedded SVG data URIs…");
        ProgressManager.getInstance().run(
                new Task.Backgroundable(project, "Searching for embedded SVG data URIs", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        EmbeddedSvgScanner.Result result = EmbeddedSvgScanner.scan(
                                project, indicator, embeddedSearchGeneration, generation,
                                path -> indicator.setText2(projectRelativePath(project.getBasePath(), path)),
                                (path, exception) ->
                                        LOG.debug("Unable to search project file for embedded SVGs: " + path,
                                                exception));
                        indicator.checkCanceled();
                        SwingUtilities.invokeLater(() -> {
                            if (!disposed && !project.isDisposed()
                                    && generation == embeddedSearchGeneration.get()) {
                                embeddedMatches.clear();
                                embeddedMatches.addAll(result.getMatches());
                                unreadableEmbeddedFiles = result.getUnreadableFiles();
                                applyEmbeddedFilter();
                            }
                        });
                    }
                });
    }

    private void applyEmbeddedFilter() {
        if (disposed) {
            return;
        }
        String query = embeddedSearchField.getText();
        embeddedListModel.clear();
        for (SvgEmbeddedSearch.Match match : embeddedMatches) {
            if (SvgEmbeddedSearch.matchesSearch(match, query)) {
                embeddedListModel.addElement(match);
            }
        }
        embeddedStatusLabel.setText(embeddedListModel.isEmpty()
                ? (embeddedMatches.isEmpty() ? "No embedded SVG data URIs found in this project"
                : "No matching embedded SVG data URIs")
                : embeddedListModel.size() + (embeddedListModel.size() == 1
                ? " embedded SVG data URI match" : " embedded SVG data URI matches"));
        if (unreadableEmbeddedFiles > 0) {
            embeddedStatusLabel.setText(embeddedStatusLabel.getText() + " · "
                    + unreadableEmbeddedFiles + " files could not be read");
        }
    }

    static boolean matchesSearch(String path, String query) {
        return query == null || query.isBlank()
                || path.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    static String projectRelativePath(String projectBasePath, String filePath) {
        String normalizedBasePath = normalizePath(projectBasePath);
        String normalizedFilePath = normalizePath(filePath);
        if (normalizedBasePath.isEmpty()) {
            return normalizedFilePath;
        }
        String baseWithSeparator = normalizedBasePath.endsWith("/")
                ? normalizedBasePath
                : normalizedBasePath + "/";
        if (normalizedFilePath.startsWith(baseWithSeparator)) {
            return normalizedFilePath.substring(baseWithSeparator.length());
        }
        if (normalizedFilePath.equals(normalizedBasePath)) {
            return "";
        }
        return normalizedFilePath;
    }

    private static String normalizePath(String path) {
        return path == null ? "" : path.replace('\\', '/').replaceAll("/+$", "");
    }

    private void applyFilter() {
        if (disposed) {
            return;
        }
        String query = searchField.getText();
        listModel.clear();
        for (VirtualFile file : projectSvgFiles) {
            if (matchesSearch(file.getPath(), query)) {
                listModel.addElement(file);
            }
        }
        statusLabel.setText(listModel.isEmpty()
                ? (projectSvgFiles.isEmpty() ? "No SVG files found in this project" : "No matching SVG files")
                : listModel.size() + (listModel.size() == 1 ? " SVG file" : " SVG files"));
    }

    private void openSelectedFile() {
        VirtualFile file = fileList.getSelectedValue();
        if (file != null && file.isValid()) {
            OpenSvgWithToolkitAction.openPreview(project, file);
        }
    }

    private void openSelectedEmbeddedPreview() {
        SvgEmbeddedSearch.Match match = embeddedList.getSelectedValue();
        if (match == null || project.isDisposed()) {
            return;
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile existingPreview = findOpenEmbeddedPreview(editorManager.getOpenFiles(), match.getDataUri());
        if (existingPreview != null) {
            editorManager.openFile(existingPreview, true);
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String svgText = SvgDataUriUtil.decodeSvgContent(match.getDataUri());
                if (svgText == null) {
                    showEmbeddedPreviewError("Unable to decode the SVG data URI.", NotificationType.WARNING);
                    return;
                }
                var preview = SvgRenderer.render(svgText, 400, 400);
                if (preview == null) {
                    showEmbeddedPreviewError("Unable to render the selected SVG data URI.", NotificationType.ERROR);
                    return;
                }
                if (project.isDisposed()) {
                    return;
                }
                SvgVirtualFile previewFile = new SvgVirtualFile(
                        "svg-preview-" + System.currentTimeMillis(),
                        svgText, match.getDataUri(), preview);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!disposed && !project.isDisposed()) {
                        FileEditorManager currentEditorManager = FileEditorManager.getInstance(project);
                        VirtualFile alreadyOpen = findOpenEmbeddedPreview(
                                currentEditorManager.getOpenFiles(), match.getDataUri());
                        currentEditorManager.openFile(alreadyOpen == null ? previewFile : alreadyOpen, true);
                    }
                });
            } catch (RuntimeException exception) {
                LOG.warn("Unable to open embedded SVG preview from " + match.getFilePath(), exception);
                showEmbeddedPreviewError("Unable to open the selected SVG preview: "
                        + exception.getMessage(), NotificationType.ERROR);
            }
        });
    }

    static VirtualFile findOpenEmbeddedPreview(VirtualFile[] openFiles, String dataUri) {
        if (openFiles == null || dataUri == null) {
            return null;
        }
        for (VirtualFile openFile : openFiles) {
            if (openFile instanceof SvgVirtualFile previewFile
                    && dataUri.equals(previewFile.getRawBase64())) {
                return openFile;
            }
        }
        return null;
    }

    private void showEmbeddedPreviewError(String message, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!disposed && !project.isDisposed()) {
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("SVG Toolkit")
                        .createNotification(message, type)
                        .notify(project);
            }
        });
    }

    private void openSelectedEmbeddedSource() {
        SvgEmbeddedSearch.Match match = embeddedList.getSelectedValue();
        if (match == null || project.isDisposed()) {
            return;
        }
        VirtualFile file = match.getSourceFile();
        if (file != null && file.isValid()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!disposed && !project.isDisposed() && file.isValid()) {
                    new OpenFileDescriptor(project, file, match.getLineNumber() - 1, 0).navigate(true);
                }
            });
        }
    }

    private void requestThumbnail(VirtualFile file) {
        String path = file.getPath();
        String key = "file:" + path;
        if (!thumbnailCache.beginLoading(key)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ImageIcon thumbnail = null;
            String dimensions = "Dimensions unavailable";
            try {
                String source = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                    try {
                        return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
                String svg = SvgDataUriUtil.decodeSvgContent(source);
                if (svg != null) {
                    String svgDimensions = SvgDimensions.formatSvgDimensions(svg);
                    if (svgDimensions != null) {
                        dimensions = svgDimensions;
                    }
                    var image = SvgRenderer.render(svg, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                    if (image != null) {
                        thumbnail = new ImageIcon(image);
                    }
                }
            } catch (RuntimeException exception) {
                LOG.debug("Unable to render SVG thumbnail: " + path, exception);
            } finally {
                if (!disposed) {
                    thumbnailCache.complete(key, thumbnail, dimensions);
                }
                SwingUtilities.invokeLater(() -> {
                    if (!disposed) {
                        fileList.repaint();
                    }
                });
            }
        });
    }

    private final class SvgFileCellRenderer extends JPanel implements ListCellRenderer<VirtualFile> {
        private final JLabel thumbnailLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();
        private final JLabel metadataLabel = new JLabel();
        private final JLabel pathLabel = new JLabel();

        SvgFileCellRenderer() {
            super(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            thumbnailLabel.setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));
            thumbnailLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 2));
            textPanel.add(nameLabel);
            textPanel.add(metadataLabel);
            textPanel.add(pathLabel);
            add(thumbnailLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends VirtualFile> list, VirtualFile file,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            String path = file.getPath();
            String key = "file:" + path;
            ImageIcon thumbnail = thumbnailCache.getThumbnail(key);
            String dimensions = thumbnailCache.getDimensions(key);
            if (thumbnail == null) {
                thumbnailLabel.setIcon(null);
                thumbnailLabel.setText("SVG");
                thumbnailLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                requestThumbnail(file);
            } else {
                thumbnailLabel.setText("");
                thumbnailLabel.setIcon(thumbnail);
            }
            nameLabel.setText(file.getName());
            nameLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            metadataLabel.setText(formatSize(file.getLength()) + " · "
                    + (dimensions == null ? "Reading dimensions…" : dimensions));
            String relativePath = projectRelativePath(project.getBasePath(), path);
            pathLabel.setText(relativePath);
            pathLabel.setToolTipText(path);
            Color secondaryForeground = UIManager.getColor("Label.disabledForeground");
            metadataLabel.setForeground(secondaryForeground);
            pathLabel.setForeground(secondaryForeground);

            Color background = isSelected
                    ? UIManager.getColor("List.selectionBackground")
                    : list.getBackground();
            Color foreground = isSelected
                    ? UIManager.getColor("List.selectionForeground")
                    : list.getForeground();
            setBackground(background);
            thumbnailLabel.setBackground(background);
            nameLabel.setForeground(foreground);
            if (isSelected) {
                metadataLabel.setForeground(foreground);
                pathLabel.setForeground(foreground);
            }
            setOpaque(true);
            return this;
        }
    }

    private final class EmbeddedSvgCellRenderer extends JPanel
            implements ListCellRenderer<SvgEmbeddedSearch.Match> {
        private final JLabel thumbnailLabel = new JLabel();
        private final JLabel pathLabel = new JLabel();
        private final JLabel lineLabel = new JLabel();
        private final JLabel snippetLabel = new JLabel();

        EmbeddedSvgCellRenderer() {
            super(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            thumbnailLabel.setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));
            thumbnailLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel locationPanel = new JPanel(new BorderLayout(8, 0));
            lineLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            lineLabel.setFont(lineLabel.getFont().deriveFont(Font.BOLD));
            lineLabel.setOpaque(true);
            locationPanel.add(lineLabel, BorderLayout.WEST);
            locationPanel.add(pathLabel, BorderLayout.CENTER);
            JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
            textPanel.add(locationPanel);
            textPanel.add(snippetLabel);
            add(thumbnailLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SvgEmbeddedSearch.Match> list,
                                                      SvgEmbeddedSearch.Match match, int index,
                                                                                                boolean isSelected, boolean cellHasFocus) {
                                                      String thumbnailKey = embeddedThumbnailKey(match);
                                                      ImageIcon thumbnail = thumbnailCache.getThumbnail(thumbnailKey);
            if (thumbnail == null) {
                thumbnailLabel.setIcon(null);
                thumbnailLabel.setText("SVG");
                thumbnailLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                requestEmbeddedThumbnail(match, thumbnailKey);
            } else {
                thumbnailLabel.setText("");
                thumbnailLabel.setIcon(thumbnail);
            }
            String relativePath = projectRelativePath(project.getBasePath(), match.getFilePath());
            pathLabel.setText(relativePath);
            pathLabel.setToolTipText(match.getFilePath());
            pathLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            lineLabel.setText("Line " + match.getLineNumber());
            lineLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            lineLabel.setToolTipText("Source line " + match.getLineNumber());
            snippetLabel.setText(match.getSnippet());
            Color background = isSelected
                    ? UIManager.getColor("List.selectionBackground")
                    : list.getBackground();
            Color foreground = isSelected
                    ? UIManager.getColor("List.selectionForeground")
                    : list.getForeground();
            setBackground(background);
            thumbnailLabel.setBackground(background);
            pathLabel.setForeground(foreground);
            lineLabel.setForeground(foreground);
            lineLabel.setBackground(background);
            snippetLabel.setForeground(isSelected ? foreground : UIManager.getColor("Label.disabledForeground"));
            setToolTipText(relativePath + ":" + match.getLineNumber());
            setOpaque(true);
            return this;
        }
    }

    private static String embeddedThumbnailKey(SvgEmbeddedSearch.Match match) {
        return "data-uri:" + match.getFilePath() + "#" + match.getOffset();
    }

    private void requestEmbeddedThumbnail(SvgEmbeddedSearch.Match match, String key) {
        if (!thumbnailCache.beginLoading(key)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ImageIcon thumbnail = null;
            try {
                String svgText = SvgDataUriUtil.decodeSvgContent(match.getDataUri());
                if (svgText != null) {
                    var image = SvgRenderer.render(svgText, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                    if (image != null) {
                        thumbnail = new ImageIcon(image);
                    }
                }
            } catch (RuntimeException exception) {
                LOG.debug("Unable to render embedded SVG thumbnail: " + match.getFilePath(), exception);
            } finally {
                if (!disposed) {
                    thumbnailCache.complete(key, thumbnail, null);
                }
                SwingUtilities.invokeLater(() -> {
                    if (!disposed) {
                        embeddedList.repaint();
                    }
                });
            }
        });
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
    }

    @Override
    public void dispose() {
        disposed = true;
        scanGeneration.incrementAndGet();
        embeddedSearchGeneration.incrementAndGet();
        thumbnailCache.clear();
    }
}
