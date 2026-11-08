package com.plugin.svg;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SvgProjectBrowserPanelTest {
    @Test
    public void filtersByFileNameOrFolderWithoutCaseSensitivity() {
        assertTrue(SvgProjectBrowserPanel.matchesSearch("/project/assets/Logo.SVG", "logo"));
        assertTrue(SvgProjectBrowserPanel.matchesSearch("/project/assets/Logo.SVG", "ASSETS"));
        assertFalse(SvgProjectBrowserPanel.matchesSearch("/project/assets/Logo.SVG", "icon"));
    }

    @Test
    public void emptySearchMatchesEveryFile() {
        assertTrue(SvgProjectBrowserPanel.matchesSearch("/project/assets/icon.svg", ""));
        assertTrue(SvgProjectBrowserPanel.matchesSearch("/project/assets/icon.svg", null));
    }

    @Test
    public void displaysPathRelativeToProjectRoot() {
        assertEquals("src/main/resources/icons/logo.svg",
                SvgProjectBrowserPanel.projectRelativePath(
                        "/Users/dev/project", "/Users/dev/project/src/main/resources/icons/logo.svg"));
    }

    @Test
    public void pathOutsideProjectRemainsAbsolute() {
        assertEquals("/Users/dev/shared/logo.svg",
                SvgProjectBrowserPanel.projectRelativePath(
                        "/Users/dev/project", "/Users/dev/shared/logo.svg"));
    }

    @Test
    public void windowsPathsAreNormalized() {
        assertEquals("assets/logo.svg",
                SvgProjectBrowserPanel.projectRelativePath(
                        "C:\\work\\project", "C:\\work\\project\\assets\\logo.svg"));
    }

    @Test
    public void findsExistingPreviewForSameDataUri() {
        String dataUri = SvgDataUriUtil.encodeBase64DataUri("<svg><path/></svg>");
        SvgVirtualFile preview = new SvgVirtualFile("preview", "<svg/>", dataUri, null);

        assertSame(preview, SvgProjectBrowserPanel.findOpenEmbeddedPreview(
                new VirtualFile[]{new LightVirtualFile("other.svg"), preview}, dataUri));
    }

    @Test
    public void doesNotReusePreviewForDifferentDataUri() {
        SvgVirtualFile preview = new SvgVirtualFile("preview", "<svg/>", "data:image/svg+xml;base64,abc", null);

        assertNull(SvgProjectBrowserPanel.findOpenEmbeddedPreview(
                new VirtualFile[]{preview}, "data:image/svg+xml;base64,def"));
    }
}
