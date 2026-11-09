package com.plugin.svg.editor;

import org.junit.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SvgDimensionsTest {
    @Test
    public void formatsExplicitPixelDimensionsAndUtf8ByteSize() {
        BufferedImage renderedImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        String source = "<svg width=\"24\" height=\"24\">é</svg>";

        assertEquals("24 × 24 px · SVG " + source.getBytes(StandardCharsets.UTF_8).length + " B",
                SvgDimensions.formatImageDetails(renderedImage, source));
        assertEquals("24 × 24 px", SvgDimensions.formatSvgDimensions(source));
    }

    @Test
    public void infersMissingDimensionsFromViewBox() {
        assertEquals("24 × 16 px", SvgDimensions.formatSvgDimensions("<svg viewBox=\"0 0 24 16\"></svg>"));
        assertEquals("48 × 16 px",
                SvgDimensions.formatSvgDimensions("<svg width=\"48\" viewBox=\"0 0 24 8\"></svg>"));
        assertEquals("24 × 16 px",
                SvgDimensions.formatSvgDimensions("<svg height=\"16\" viewBox=\"0 0 12 8\"></svg>"));
    }

    @Test
    public void convertsPhysicalUnitsToPixels() {
        assertEquals("96 × 48 px", SvgDimensions.formatSvgDimensions("<svg width=\"1in\" height=\"1.27cm\"/>"));
        assertEquals("96 × 96 px", SvgDimensions.formatSvgDimensions("<svg width=\"72pt\" height=\"6pc\"/>"));
        assertEquals("96 × 96 px", SvgDimensions.formatSvgDimensions("<svg width=\"25.4mm\" height=\"1in\"/>"));
    }

    @Test
    public void returnsNullWhenDimensionsCannotBeDetermined() {
        assertNull(SvgDimensions.formatSvgDimensions(null));
        assertNull(SvgDimensions.formatSvgDimensions("not SVG"));
        assertNull(SvgDimensions.formatSvgDimensions("<svg width=\"auto\" height=\"0\"></svg>"));
        assertNull(SvgDimensions.formatSvgDimensions("<svg viewBox=\"0 0 0 10\"></svg>"));
    }

    @Test
    public void scalesRasterDimensionsWithinMaximumAndPreservesAspectRatio() {
        assertEquals(new Dimension(64, 32),
                SvgDimensions.getRasterDimensions("<svg width=\"40\" height=\"20\"/>", 64));
        assertEquals(new Dimension(48, 32),
                SvgDimensions.getRasterDimensions("<svg viewBox=\"0 0 24 16\"/>", 48));
    }

    @Test
    public void usesSquareOutputWhenSvgHasNoIntrinsicDimensions() {
        assertEquals(new Dimension(48, 48), SvgDimensions.getRasterDimensions("<svg/>", 48));
    }

    @Test
    public void fallsBackToRenderedImageSizeAndFormatsLargeSourceSize() {
        BufferedImage renderedImage = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB);
        assertTrue(SvgDimensions.formatImageDetails(renderedImage, "<svg/>")
                .startsWith("24 × 16 px · SVG "));

        String source = "<svg>" + "x".repeat(1024 * 1024) + "</svg>";
        assertTrue(SvgDimensions.formatImageDetails(renderedImage, source).endsWith("SVG 1.00 MB"));
    }
}
