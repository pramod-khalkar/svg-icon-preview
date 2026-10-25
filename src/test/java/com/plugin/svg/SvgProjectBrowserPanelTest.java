package com.plugin.svg;

import org.junit.Test;

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
}
