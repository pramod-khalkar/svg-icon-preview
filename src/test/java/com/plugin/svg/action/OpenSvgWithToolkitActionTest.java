package com.plugin.svg.action;

import com.plugin.svg.editor.SvgVirtualFile;
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

    @Test
    public void reusesOpenPreviewForTheSameSourceFile() {
        LightVirtualFile source = new LightVirtualFile("icon.svg");
        SvgVirtualFile preview = new SvgVirtualFile(
                "svg-preview-icon.svg-1", "<svg/>", "data-uri", null,
                "icon.svg", source, "<svg/>");

        assertSame(preview, OpenSvgWithToolkitAction.findOpenPreview(
                new VirtualFile[]{new LightVirtualFile("other.svg"), preview}, source));
    }

    @Test
    public void doesNotMatchPreviewForAnotherSourceFile() {
        LightVirtualFile source = new LightVirtualFile("icon.svg");
        LightVirtualFile otherSource = new LightVirtualFile("other.svg");
        SvgVirtualFile preview = new SvgVirtualFile(
                "svg-preview-other.svg-1", "<svg/>", "data-uri", null,
                "other.svg", otherSource, "<svg/>");

        assertNull(OpenSvgWithToolkitAction.findOpenPreview(new VirtualFile[]{preview}, source));
    }
}
