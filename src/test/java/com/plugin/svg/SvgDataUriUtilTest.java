package com.plugin.svg;

import org.junit.Test;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

public class SvgDataUriUtilTest {

    private static final String SIMPLE_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"2\" height=\"2\"><rect width=\"2\" height=\"2\" fill=\"#000\"/></svg>";

    @Test
    public void testDecodeStandardBase64() {
        String b64 = java.util.Base64.getEncoder().encodeToString(SIMPLE_SVG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String decoded = SvgDataUriUtil.decodePayload(b64, true);
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
    }

    @Test
    public void testDecodeUrlSafeBase64() {
        String b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(SIMPLE_SVG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String decoded = SvgDataUriUtil.decodePayload(b64, true);
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
    }

    @Test
    public void testDecodeMissingPadding() {
        String b64 = java.util.Base64.getEncoder().encodeToString(SIMPLE_SVG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // remove padding
        b64 = b64.replaceAll("=+$", "");
        String decoded = SvgDataUriUtil.decodePayload(b64, true);
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
    }

    @Test
    public void testUrlEncodedSvg() {
        String urlEncoded = java.net.URLEncoder.encode(SIMPLE_SVG, java.nio.charset.StandardCharsets.UTF_8);
        String decoded = SvgDataUriUtil.decodePayload(urlEncoded, false);
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
    }

    @Test
    public void testPlainSvgText() {
        String decoded = SvgDataUriUtil.decodePayload(SIMPLE_SVG, false);
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
    }

    @Test
    public void testSvgRenderer() {
        BufferedImage img = SvgRenderer.render(SIMPLE_SVG, 16, 16);
        assertNotNull(img);
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
    }

    @Test
    public void testPayloadWithTrailingQuote() {
        String base64Payload = java.util.Base64.getEncoder().encodeToString(SIMPLE_SVG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // String literal representation: "data:image/svg+xml;base64,PHN2Zy..."
        String elementText = "\"data:image/svg+xml;base64," + base64Payload + "\"";
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)data:image/svg\\+xml(?:;charset=[^,;]+)?(?:;(base64))?,(.*)", java.util.regex.Pattern.DOTALL).matcher(elementText);
        assertTrue(m.find());
        
        String marker = m.group(1);
        String payload = m.group(2);
        
        // This simulates the behavior of SvgIconLineMarkerProvider
        String normalizedPayload = SvgDataUriUtil.normalizePayload(payload);
        String decoded = SvgDataUriUtil.decodePayload(normalizedPayload, marker != null);
        
        assertNotNull(decoded);
        // It is just the raw base64 payload, not actual SVG!
        assertFalse("Decoded payload should not contain SVG tag because decoding failed", decoded.contains("<svg"));
        
        // And rendering it should fail (return null)
        BufferedImage img = SvgRenderer.render(decoded, 16, 16);
        assertNull("Rendering should fail for raw base64 string", img);
    }

    @Test
    public void testCleanPayloadWithoutQuotes() {
        String base64Payload = java.util.Base64.getEncoder().encodeToString(SIMPLE_SVG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Unescaped value representation: data:image/svg+xml;base64,PHN2Zy...
        String cleanValue = "data:image/svg+xml;base64," + base64Payload;
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)data:image/svg\\+xml(?:;charset=[^,;]+)?(?:;(base64))?,(.*)", java.util.regex.Pattern.DOTALL).matcher(cleanValue);
        assertTrue(m.find());
        
        String marker = m.group(1);
        String payload = m.group(2);
        
        String normalizedPayload = SvgDataUriUtil.normalizePayload(payload);
        String decoded = SvgDataUriUtil.decodePayload(normalizedPayload, marker != null);
        
        assertNotNull(decoded);
        assertTrue(decoded.contains("<svg"));
        
        BufferedImage img = SvgRenderer.render(decoded, 16, 16);
        assertNotNull(img);
        assertEquals(16, img.getWidth());
    }

    @Test
    public void testLargeSvgRenderer() {
        // Construct a large SVG (> 200KB) by repeating elements
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"300\">");
        for (int i = 0; i < 5000; i++) {
            sb.append("<rect x=\"").append(i % 100).append("\" y=\"").append((i / 100) * 5)
              .append("\" width=\"4\" height=\"4\" fill=\"#").append(String.format("%06x", i * 100 & 0xFFFFFF)).append("\"/>");
        }
        sb.append("</svg>");
        String largeSvg = sb.toString();
        assertTrue("SVG size should be > 200KB", largeSvg.length() > 200000);

        BufferedImage img = SvgRenderer.render(largeSvg, 300, 300);
        assertNotNull("Large SVG should render successfully", img);
        assertEquals(300, img.getWidth());
    }

    @Test
    public void testUrlDecodePreservesPlus() {
        String svgWithPlus = "<svg><path d=\"M10+20\"/></svg>";
        String decoded = SvgDataUriUtil.decodePayload(svgWithPlus, false);
        assertNotNull(decoded);
        assertTrue("Decoded SVG should preserve '+' character", decoded.contains("M10+20"));
    }
}

