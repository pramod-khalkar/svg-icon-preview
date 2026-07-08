package com.plugin.svg;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import com.plugin.svg.SvgPreviewToolWindow;

/** Factory to create the tool window. */
public class SvgPreviewToolWindowFactory implements ToolWindowFactory {
    private static SvgPreviewToolWindow INSTANCE;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (INSTANCE == null) {
            INSTANCE = new SvgPreviewToolWindow();
        }
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(INSTANCE, "", false);
        toolWindow.getContentManager().removeAllContents(true);
        toolWindow.getContentManager().addContent(content);
    }

    /** Returns the singleton instance for updating preview. */
    static SvgPreviewToolWindow getInstance() {
        return INSTANCE;
    }
}