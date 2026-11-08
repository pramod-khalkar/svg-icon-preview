package com.plugin.svg.browser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ProjectSvgFileScanner {
    private ProjectSvgFileScanner() {
    }

    static List<VirtualFile> scan(Project project, ProgressIndicator indicator) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<VirtualFile>>) () -> {
            List<VirtualFile> files = new ArrayList<>();
            ProjectFileIndex.getInstance(project).iterateContent(file -> {
                indicator.checkCanceled();
                if (!file.isDirectory() && isSvgFile(file)) {
                    files.add(file);
                }
                return true;
            });
            files.sort(Comparator.comparing(VirtualFile::getPath, String.CASE_INSENSITIVE_ORDER));
            return files;
        });
    }

    static boolean isSvgFile(VirtualFile file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".svg");
    }
}
