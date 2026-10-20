package com.plugin.svg;

import com.intellij.ui.OnePixelSplitter;
import org.junit.Test;

import javax.swing.JPanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntellijSplitterLayoutTest {

    @Test
    public void horizontalSplitterResizesSideBySidePanes() {
        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.5f, 0.1f, 0.9f);
        JPanel source = new JPanel();
        JPanel preview = new JPanel();
        splitter.setFirstComponent(source);
        splitter.setSecondComponent(preview);
        splitter.setDividerWidth(8);
        splitter.setResizeEnabled(true);
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setSize(600, 300);
        splitter.doLayout();

        int initialSourceWidth = source.getWidth();
        assertTrue(initialSourceWidth > 0);
        assertTrue(preview.getWidth() > 0);
        assertTrue(source.getX() < preview.getX());

        splitter.setProportion(0.65f);
        splitter.doLayout();

        assertTrue("Changing the splitter proportion should resize the source pane",
                source.getWidth() > initialSourceWidth);
        assertEquals(0.65f, splitter.getProportion(), 0.001f);
    }
}
