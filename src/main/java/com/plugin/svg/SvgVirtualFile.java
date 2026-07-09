package com.plugin.svg;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.LightVirtualFileBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;

/**
 * A lightweight virtual file that carries SVG payload and rendered image
 * for opening in an IDE editor tab.
 *
 * @author Pramod Khalkar
 */
public class SvgVirtualFile extends VirtualFile {

    private final String displayName;
    private final String svgText;      // decoded SVG markup
    private final String rawBase64;    // original base64 payload shown in left pane
    private final BufferedImage previewImage;

    public SvgVirtualFile(String displayName, String svgText, String rawBase64, BufferedImage previewImage) {
        this.displayName = displayName;
        this.svgText = svgText;
        this.rawBase64 = rawBase64;
        this.previewImage = previewImage;
    }

    public String getSvgText() { return svgText; }
    public String getRawBase64() { return rawBase64; }
    public BufferedImage getPreviewImage() { return previewImage; }

    @Override public @NotNull String getName() { return displayName; }
    @Override public @NotNull VirtualFileSystem getFileSystem() { return SvgVirtualFileSystem.INSTANCE; }
    @Override public @NotNull String getPath() { return "/" + displayName; }
    @Override public boolean isWritable() { return false; }
    @Override public boolean isDirectory() { return false; }
    @Override public boolean isValid() { return true; }
    @Override public VirtualFile getParent() { return null; }
    @Override public VirtualFile[] getChildren() { return VirtualFile.EMPTY_ARRAY; }
    @Override public @NotNull byte[] contentsToByteArray() throws IOException { return rawBase64.getBytes(StandardCharsets.UTF_8); }
    @Override public long getTimeStamp() { return System.currentTimeMillis(); }
    @Override public long getLength() { return rawBase64.length(); }
    @Override public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {}
    @Override public @NotNull InputStream getInputStream() throws IOException { return InputStream.nullInputStream(); }
    @Override public long getModificationStamp() { return System.currentTimeMillis(); }

    @Override
    public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull FileType getFileType() {
        return StdFileTypes.XML;
    }
}
