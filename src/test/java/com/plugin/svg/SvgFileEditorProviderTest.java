package com.plugin.svg;

import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.project.DumbAware;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SvgFileEditorProviderTest {

    @Test
    public void hidesDefaultTextEditorTab() {
        SvgFileEditorProvider provider = new SvgFileEditorProvider();
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, provider.getPolicy());
        org.junit.Assert.assertTrue(provider instanceof DumbAware);
    }
}
