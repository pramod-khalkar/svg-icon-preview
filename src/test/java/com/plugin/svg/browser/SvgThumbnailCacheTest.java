package com.plugin.svg.browser;

import org.junit.Test;

import javax.swing.ImageIcon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SvgThumbnailCacheTest {
    @Test
    public void cachesThumbnailsAndDimensionsTogether() {
        SvgThumbnailCache cache = new SvgThumbnailCache(2);
        ImageIcon icon = new ImageIcon();

        assertTrue(cache.beginLoading("logo.svg"));
        cache.complete("logo.svg", icon, "24 × 24 px");

        assertSame(icon, cache.getThumbnail("logo.svg"));
        assertEquals("24 × 24 px", cache.getDimensions("logo.svg"));
        assertFalse(cache.beginLoading("logo.svg"));
    }

    @Test
    public void onlyAllowsOneConcurrentLoadPerKey() {
        SvgThumbnailCache cache = new SvgThumbnailCache(2);

        assertTrue(cache.beginLoading("logo.svg"));
        assertFalse(cache.beginLoading("logo.svg"));
        cache.complete("logo.svg", new ImageIcon(), null);
        assertTrue(cache.beginLoading("other.svg"));
    }

    @Test
    public void boundsCacheByLeastRecentlyUsedEntry() {
        SvgThumbnailCache cache = new SvgThumbnailCache(2);
        ImageIcon first = new ImageIcon();
        cache.complete("first", first, "1 × 1 px");
        cache.complete("second", new ImageIcon(), "2 × 2 px");

        assertSame(first, cache.getThumbnail("first"));
        cache.complete("third", new ImageIcon(), "3 × 3 px");

        assertNull(cache.getThumbnail("second"));
        assertNull(cache.getDimensions("second"));
        assertSame(first, cache.getThumbnail("first"));
    }

    @Test
    public void cachesFailedRendersUntilEvictedOrCleared() {
        SvgThumbnailCache cache = new SvgThumbnailCache(1);
        assertTrue(cache.beginLoading("broken.svg"));
        cache.complete("broken.svg", null, "Dimensions unavailable");

        assertNull(cache.getThumbnail("broken.svg"));
        assertEquals("Dimensions unavailable", cache.getDimensions("broken.svg"));
        assertFalse(cache.beginLoading("broken.svg"));
        cache.clear();
        assertTrue(cache.beginLoading("broken.svg"));
    }
}
