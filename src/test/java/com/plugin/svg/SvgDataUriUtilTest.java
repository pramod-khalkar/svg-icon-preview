package com.plugin.svg;

import org.junit.Test;

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
}
