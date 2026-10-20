package com.plugin.svg;

import com.intellij.openapi.fileEditor.FileEditorPolicy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SvgFileEditorProviderTest {

    @Test
    public void hidesDefaultTextEditorTab() {
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, new SvgFileEditorProvider().getPolicy());
    }
}
