package com.plugin.svg;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

    @Test
    public void actionReadsProjectViewSelectionOnTheEdt() {
        assertTrue(new OpenSvgWithToolkitAction().getActionUpdateThread() == ActionUpdateThread.EDT);
    }

    @Test
    public void selectedFileFilteringIgnoresNonSvgAndDirectories() {
        LightVirtualFile nonSvg = new LightVirtualFile("icon.png");
        LightVirtualFile svg = new LightVirtualFile("icon.SVG");

        assertSame(svg, OpenSvgWithToolkitAction.findSvgFile(new VirtualFile[]{nonSvg, svg}));
        assertNull(OpenSvgWithToolkitAction.findSvgFile(new VirtualFile[]{nonSvg}));
    }

    @Test
    public void selectedItemsAreResolvedToSvgFiles() {
        LightVirtualFile svg = new LightVirtualFile("icon.svg");
        LightVirtualFile nonSvg = new LightVirtualFile("icon.png");

        assertSame(svg, OpenSvgWithToolkitAction.selectedSvgFromItem(svg));
        assertNull(OpenSvgWithToolkitAction.selectedSvgFromItem(nonSvg));
    }
}
