package com.plugin.svg.settings;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SvgToolkitSettingsTest {
    @Test
    public void releaseNotesAreShownOncePerVersion() {
        SvgToolkitSettings settings = new SvgToolkitSettings();

        assertTrue(settings.markReleaseNotesShownIfNeeded("1.2.0"));
        assertFalse(settings.markReleaseNotesShownIfNeeded("1.2.0"));
        assertTrue(settings.markReleaseNotesShownIfNeeded("1.2.1"));
    }
}
