package com.plugin.svg;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
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
    public void update(@NotNull AnActionEvent event) {
        VirtualFile file = getSelectedSvgFile(event);
        boolean hasFileSelection = event.getData(CommonDataKeys.VIRTUAL_FILE) != null
                || hasVirtualFileSelection(event)
                || event.getData(CommonDataKeys.PSI_FILE) != null
                || event.getData(CommonDataKeys.PSI_ELEMENT) != null;
        event.getPresentation().setEnabledAndVisible(file != null || !hasFileSelection);
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

    private boolean hasVirtualFileSelection(AnActionEvent event) {
        VirtualFile[] selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        return selectedFiles != null && selectedFiles.length > 0;
    }

    static boolean isSvgFileName(String fileName) {
        return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".svg");
    }

    private VirtualFile getSelectedSvgFile(AnActionEvent event) {
        VirtualFile[] selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles != null) {
            for (VirtualFile file : selectedFiles) {
                if (file != null && !file.isDirectory() && isSvgFileName(file.getName())) {
                    return file;
                }
            }
        }

        VirtualFile selectedFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedFile != null && !selectedFile.isDirectory() && isSvgFileName(selectedFile.getName())) {
            return selectedFile;
        }

        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null && psiFile.getVirtualFile() != null
                && isSvgFileName(psiFile.getVirtualFile().getName())) {
            return psiFile.getVirtualFile();
        }

        PsiElement psiElement = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement != null) {
            PsiFile containingFile = psiElement.getContainingFile();
            VirtualFile containingVirtualFile = containingFile == null ? null : containingFile.getVirtualFile();
            if (containingVirtualFile != null && isSvgFileName(containingVirtualFile.getName())) {
                return containingVirtualFile;
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
            SvgVirtualFile previewFile = new SvgVirtualFile(previewName, svgText, source, preview);
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
