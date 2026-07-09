package com.plugin.svg;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Minimal virtual file system for synthetic SVG preview files.
 *
 * @author Pramod Khalkar
 */
public class SvgVirtualFileSystem extends VirtualFileSystem {

    public static final SvgVirtualFileSystem INSTANCE = new SvgVirtualFileSystem();
    private static final String PROTOCOL = "svg-preview";

    private SvgVirtualFileSystem() {}

    @Override public @NotNull @NonNls String getProtocol() { return PROTOCOL; }
    @Override public @Nullable VirtualFile findFileByPath(@NotNull @NonNls String path) { return null; }
    @Override public void refresh(boolean asynchronous) {}
    @Override public @Nullable VirtualFile refreshAndFindFileByPath(@NotNull String path) { return null; }
    @Override public void addVirtualFileListener(@NotNull VirtualFileListener listener) {}
    @Override public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {}
    @Override protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {}
    @Override protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException {}
    @Override protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException {}
    @Override protected @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException { throw new UnsupportedOperationException(); }
    @Override protected @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException { throw new UnsupportedOperationException(); }
    @Override protected @NotNull VirtualFile copyFile(Object requestor, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException { throw new UnsupportedOperationException(); }
    @Override public boolean isReadOnly() { return true; }
}
