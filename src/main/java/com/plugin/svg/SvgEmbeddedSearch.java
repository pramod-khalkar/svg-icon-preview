package com.plugin.svg;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SvgEmbeddedSearch {
    private static final Pattern DATA_URI_PATTERN = Pattern.compile(
            "(?i)data:image/svg\\+xml(?:;charset=[^,;\\s\"'<>]+)?(?:;(base64))?,([A-Za-z0-9+/=_%-]+)");

    private SvgEmbeddedSearch() {
    }

    static List<Match> findMatches(String text) {
        return findMatches("", text);
    }

    static List<Match> findMatches(String filePath, String text) {
        List<Match> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }

        Matcher matcher = DATA_URI_PATTERN.matcher(text);
        int lineNumber = 1;
        int previousOffset = 0;
        while (matcher.find()) {
            for (int i = previousOffset; i < matcher.start(); i++) {
                if (text.charAt(i) == '\n') {
                    lineNumber++;
                }
            }
            previousOffset = matcher.start();

            String dataUri = matcher.group();
            String decodedSvg = SvgDataUriUtil.decodeSvgContent(dataUri);
            if (isSvg(decodedSvg)) {
                matches.add(new Match(filePath, lineNumber, matcher.start(), dataUri,
                        getLineSnippet(text, matcher.start(), matcher.end())));
            }
        }
        return matches;
    }

    static boolean matchesSearch(Match match, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return match.filePath.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || match.snippet.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || Integer.toString(match.lineNumber).contains(normalizedQuery);
    }

    private static boolean isSvg(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        int rootStart = normalized.indexOf("<svg");
        int rootEnd = normalized.indexOf('>', rootStart);
        return rootStart >= 0 && rootEnd > rootStart
                && (normalized.contains("</svg") || normalized.substring(rootStart, rootEnd + 1).trim().endsWith("/>"));
    }

    private static String getLineSnippet(String text, int start, int end) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int lineEnd = text.indexOf('\n', end);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        String line = text.substring(lineStart, lineEnd).trim();
        return line.length() <= 180 ? line : line.substring(0, 177) + "...";
    }

    static final class Match {
        private final String filePath;
        private final int lineNumber;
        private final int offset;
        private final String dataUri;
        private final String snippet;
        private final VirtualFile sourceFile;

        Match(String filePath, int lineNumber, int offset, String dataUri, String snippet) {
            this(filePath, lineNumber, offset, dataUri, snippet, null);
        }

        private Match(String filePath, int lineNumber, int offset, String dataUri, String snippet,
                      VirtualFile sourceFile) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.offset = offset;
            this.dataUri = dataUri;
            this.snippet = snippet;
            this.sourceFile = sourceFile;
        }

        String getFilePath() {
            return filePath;
        }

        int getLineNumber() {
            return lineNumber;
        }

        int getOffset() {
            return offset;
        }

        String getDataUri() {
            return dataUri;
        }

        String getSnippet() {
            return snippet;
        }

        VirtualFile getSourceFile() {
            return sourceFile;
        }

        Match withSourceFile(VirtualFile file) {
            return new Match(filePath, lineNumber, offset, dataUri, snippet, file);
        }
    }
}
