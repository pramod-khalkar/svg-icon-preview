package com.plugin.svg;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for SVG Toolkit plugin.
 * Stores user preferences in workspace settings file.
 */
@State(
    name = "SvgToolkitSettings",
    storages = @Storage("svgToolkit.xml")
)
public class SvgToolkitSettings implements PersistentStateComponent<SvgToolkitSettings> {
    // Default 200 KB threshold for automatic background rendering
    public static final int DEFAULT_MAX_INLINE_SIZE_KB = 200;

    /**
     * Maximum size (in KB) for inline SVG rendering.
     * SVGs <= this size render immediately.
     * SVGs > this size show a placeholder and render on-demand.
     */
    public int maxInlineSizeKb = DEFAULT_MAX_INLINE_SIZE_KB;

    @Nullable
    @Override
    public SvgToolkitSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SvgToolkitSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * Get the configured max inline size in bytes.
     */
    public long getMaxInlineSizeBytes() {
        return (long) maxInlineSizeKb * 1024;
    }

    /**
     * Reset to default settings.
     */
    public void resetToDefaults() {
        maxInlineSizeKb = DEFAULT_MAX_INLINE_SIZE_KB;
    }
}
