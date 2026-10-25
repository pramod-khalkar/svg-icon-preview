package com.plugin.svg;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

final class SvgProjectBrowserPanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(SvgProjectBrowserPanel.class);
    private static final int THUMBNAIL_SIZE = 64;
    private static final int MAX_CACHED_THUMBNAILS = 200;

    private final Project project;
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
    private final JList<VirtualFile> fileList = new JList<>(listModel);
    private final List<VirtualFile> projectSvgFiles = new ArrayList<>();
    private final AtomicLong scanGeneration = new AtomicLong();
    private final Set<String> thumbnailsInProgress = new HashSet<>();
    private final java.util.Map<String, String> dimensionsCache = new HashMap<>();
    private final LinkedHashMap<String, ImageIcon> thumbnailCache =
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, ImageIcon> eldest) {
                    return size() > MAX_CACHED_THUMBNAILS;
                }
            };
    private volatile boolean disposed;

    SvgProjectBrowserPanel(Project project) {
        super(new BorderLayout(0, 6));
        this.project = project;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchField.setToolTipText("Filter project SVG files by name or folder");
        searchPanel.add(searchField, BorderLayout.CENTER);
        JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
        refreshButton.setToolTipText("Refresh project SVG files");
        refreshButton.addActionListener(event -> refresh());
        searchPanel.add(refreshButton, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(searchPanel, BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

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
        add(new JScrollPane(fileList), BorderLayout.CENTER);

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
    }

    void refresh() {
        long generation = scanGeneration.incrementAndGet();
        statusLabel.setText("Searching project for SVG files…");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Searching for SVG files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                List<VirtualFile> files = ReadAction.compute(() -> {
                    List<VirtualFile> found = new ArrayList<>();
                    ProjectFileIndex.getInstance(project).iterateContent(file -> {
                        indicator.checkCanceled();
                        if (!file.isDirectory() && isSvgFile(file)) {
                            found.add(file);
                        }
                        return true;
                    });
                    found.sort((first, second) ->
                            first.getPath().compareToIgnoreCase(second.getPath()));
                    return found;
                });
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

    private static boolean isSvgFile(VirtualFile file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".svg");
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

    private void requestThumbnail(VirtualFile file) {
        String path = file.getPath();
        synchronized (thumbnailCache) {
            if (thumbnailCache.containsKey(path) || !thumbnailsInProgress.add(path)) {
                return;
            }
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ImageIcon thumbnail = null;
            String dimensions = "Dimensions unavailable";
            try {
                String source = ReadAction.compute(() -> {
                    try {
                        return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
                String svg = SvgDataUriUtil.decodeSvgContent(source);
                if (svg != null) {
                    String svgDimensions = SvgFileEditor.formatSvgDimensions(svg);
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
                synchronized (thumbnailCache) {
                    thumbnailsInProgress.remove(path);
                    dimensionsCache.put(path, dimensions);
                    if (thumbnail != null) {
                        thumbnailCache.put(path, thumbnail);
                    }
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
            ImageIcon thumbnail;
            String dimensions;
            synchronized (thumbnailCache) {
                thumbnail = thumbnailCache.get(path);
                dimensions = dimensionsCache.get(path);
            }
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
        synchronized (thumbnailCache) {
            thumbnailCache.clear();
            dimensionsCache.clear();
            thumbnailsInProgress.clear();
        }
    }
}
