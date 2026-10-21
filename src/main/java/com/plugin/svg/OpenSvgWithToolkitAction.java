package com.plugin.svg;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OpenSvgWithToolkitAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(OpenSvgWithToolkitAction.class);
    private static final Icon SVG_TOOLKIT_ICON =
            IconLoader.getIcon("/icons/svgToolkit.svg", OpenSvgWithToolkitAction.class);

    public OpenSvgWithToolkitAction() {
        getTemplatePresentation().setIcon(SVG_TOOLKIT_ICON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = getSelectedSvgFile(event);
        event.getPresentation().setEnabledAndVisible(file != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        VirtualFile file = getSelectedSvgFile(event);
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        if (file == null) {
            showNotification(project, "Select an SVG file in the Project view to open it with SVG Toolkit.",
                    NotificationType.WARNING);
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> openPreview(project, file));
    }

    static boolean isSvgFileName(String fileName) {
        return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".svg");
    }

    private VirtualFile getSelectedSvgFile(AnActionEvent event) {
        VirtualFile file = findSvgFile(event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));
        if (file == null) {
            file = selectedSvgFromItem(event.getData(CommonDataKeys.VIRTUAL_FILE));
        }
        if (file != null) {
            return file;
        }

        PsiElement[] psiElements = event.getData(PlatformCoreDataKeys.PSI_ELEMENT_ARRAY);
        if (psiElements != null) {
            for (PsiElement element : psiElements) {
                file = selectedSvgFromItem(element);
                if (file != null) {
                    return file;
                }
            }
        }

        Object[] selectedItems = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
        if (selectedItems != null) {
            for (Object item : selectedItems) {
                file = selectedSvgFromItem(item);
                if (file != null) {
                    return file;
                }
            }
        }

        file = selectedSvgFromItem(event.getData(PlatformCoreDataKeys.SELECTED_ITEM));
        if (file != null) {
            return file;
        }

        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            file = selectedSvgFromItem(psiFile);
            if (file != null) {
                return file;
            }
        }

        file = selectedSvgFromItem(event.getData(CommonDataKeys.PSI_ELEMENT));
        if (file != null) {
            return file;
        }

        return getProjectViewSelectedSvgFile(event.getProject());
    }

    static VirtualFile findSvgFile(VirtualFile[] files) {
        if (files != null) {
            for (VirtualFile file : files) {
                if (isSvgFile(file)) {
                    return file;
                }
            }
        }
        return null;
    }

    static VirtualFile selectedSvgFromItem(Object item) {
        if (item instanceof VirtualFile file) {
            return isSvgFile(file) ? file : null;
        }
        if (item instanceof ProjectViewNode<?> node) {
            VirtualFile file = node.getVirtualFile();
            if (isSvgFile(file)) {
                return file;
            }
            return selectedSvgFromItem(node.getValue());
        }
        if (item instanceof AbstractTreeNode<?> node) {
            return selectedSvgFromItem(node.getValue());
        }
        if (item instanceof PsiElement element) {
            return ReadAction.compute(() -> {
                PsiFile psiFile = element instanceof PsiFile
                        ? (PsiFile) element
                        : element.getContainingFile();
                VirtualFile file = psiFile == null ? null : psiFile.getVirtualFile();
                return isSvgFile(file) ? file : null;
            });
        }
        return null;
    }

    private static boolean isSvgFile(VirtualFile file) {
        return file != null && !file.isDirectory() && isSvgFileName(file.getName());
    }

    private static VirtualFile getProjectViewSelectedSvgFile(Project project) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        var pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane == null) {
            return null;
        }
        for (Object selectedItem : pane.getSelectedUserObjects()) {
            VirtualFile file = selectedSvgFromItem(selectedItem);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private void openPreview(Project project, VirtualFile file) {
        try {
            String source = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            String svgText = SvgDataUriUtil.decodeSvgContent(source);
            if (svgText == null) {
                showNotification(project, "The selected file does not contain valid SVG content.",
                        NotificationType.WARNING);
                return;
            }

            BufferedImage preview = SvgRenderer.render(svgText, 400, 400);
            String previewName = "svg-preview-" + file.getName() + "-" + System.currentTimeMillis();
            SvgVirtualFile previewFile = new SvgVirtualFile(previewName, svgText, source, preview, file.getName());
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    FileEditorManager.getInstance(project).openFile(previewFile, true);
                }
            });
        } catch (IOException | RuntimeException exception) {
            LOG.warn("Unable to open SVG file in SVG Toolkit: " + file.getPath(), exception);
            showNotification(project, "Unable to open SVG file: " + exception.getMessage(),
                    NotificationType.ERROR);
        }
    }

    private void showNotification(Project project, String message, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("SVG Toolkit")
                        .createNotification(message, type)
                        .notify(project);
            }
        });
    }
}
