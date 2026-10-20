package com.plugin.svg;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * File editor provider for SVG previews.
 * Creates the custom SVG source and preview editor without the default text editor tab.
 */
public class SvgFileEditorProvider implements FileEditorProvider, DumbAware {
    @NotNull
    @Override
    public String getEditorTypeId() {
        return "SvgFileEditor";
    }

    @Nullable
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new SvgFileEditor(project, virtualFile);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // This editor only accepts our special virtual files for SVG previews
        return virtualFile.getName().startsWith("svg-preview-");
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}