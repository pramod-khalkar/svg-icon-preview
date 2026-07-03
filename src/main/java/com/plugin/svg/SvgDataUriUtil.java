package com.plugin.svg;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SvgDataUriUtil {

    /**
     * Normalize payload: trim, remove surrounding quotes if any
     */
    public static String normalizePayload(String payload) {
        if (payload == null) return null;
        String s = payload.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Try to decode a payload that may be base64 (standard or URL-safe) or plain/url-encoded SVG text.
     * Returns decoded string or null on failure.
     */
    public static String decodePayload(String payload, boolean explicitlyBase64) {
        if (payload == null) return null;
        String p = normalizePayload(payload);

        // If explicitly base64, try base64 first
        if (explicitlyBase64) {
            String decoded = tryBase64Decoders(p);
            if (decoded != null) return decoded;
            // fallback to url-decode then return as-is
            String urlDecoded = tryUrlDecode(p);
            return urlDecoded;
        }

        // Try as standard base64 first
        String decoded = tryBase64Decoders(p);
        if (decoded != null) return decoded;

        // If not base64, try URL-decoding (plain SVG text encoded)
        String urlDecoded = tryUrlDecode(p);
        if (looksLikeSvg(urlDecoded)) return urlDecoded;

        // As last resort, if it contains '<svg' return as-is
        if (looksLikeSvg(p)) return p;

        return null;
    }

    private static String tryBase64Decoders(String s) {
        try {
            byte[] bytes = Base64.getDecoder().decode(fixPadding(s));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
        }
        // try URL-safe
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(fixPadding(s));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private static String tryUrlDecode(String s) {
        try {
            String decoded = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            return decoded;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean looksLikeSvg(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase();
        return lower.contains("<svg") && lower.contains("</svg>") || lower.contains("<svg ");
    }

    private static String fixPadding(String s) {
        // remove whitespace
        String t = s.replaceAll("\\s+", "");
        int mod = t.length() % 4;
        if (mod != 0) {
            int pad = 4 - mod;
            StringBuilder sb = new StringBuilder(t);
            for (int i = 0; i < pad; i++) sb.append('=');
            return sb.toString();
        }
        return t;
    }
}
