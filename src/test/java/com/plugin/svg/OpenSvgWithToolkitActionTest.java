package com.plugin.svg;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenSvgWithToolkitActionTest {

    @Test
    public void onlyRecognizesSvgFileNames() {
        assertTrue(OpenSvgWithToolkitAction.isSvgFileName("icon.svg"));
        assertTrue(OpenSvgWithToolkitAction.isSvgFileName("ICON.SVG"));
        assertFalse(OpenSvgWithToolkitAction.isSvgFileName("icon.svg.txt"));
        assertFalse(OpenSvgWithToolkitAction.isSvgFileName("icon.png"));
    }

    @Test
    public void actionLoadsItsMenuIcon() {
        OpenSvgWithToolkitAction action = new OpenSvgWithToolkitAction();
        assertTrue(action.getTemplatePresentation().getIcon() != null);
    }
}
