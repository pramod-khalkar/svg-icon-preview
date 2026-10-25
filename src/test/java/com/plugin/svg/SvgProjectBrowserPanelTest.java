package com.plugin.svg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
