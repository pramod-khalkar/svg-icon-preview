package com.plugin.svg.browser;

import com.intellij.testFramework.LightVirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectSvgFileScannerTest {
    @Test
    public void recognizesSvgExtensionWithoutCaseSensitivity() {
        assertTrue(ProjectSvgFileScanner.isSvgFile(new LightVirtualFile("icon.svg")));
        assertTrue(ProjectSvgFileScanner.isSvgFile(new LightVirtualFile("ICON.SVG")));
        assertFalse(ProjectSvgFileScanner.isSvgFile(new LightVirtualFile("icon.svg.txt")));
        assertFalse(ProjectSvgFileScanner.isSvgFile(new LightVirtualFile("icon.png")));
    }
}
