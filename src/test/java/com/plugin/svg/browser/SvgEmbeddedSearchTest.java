package com.plugin.svg.browser;

import com.plugin.svg.data.SvgDataUriUtil;
import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SvgEmbeddedSearchTest {
    @Test
    public void findsValidDataUrisAndReportsSourceLines() {
        String firstUri = SvgDataUriUtil.encodeBase64DataUri("<svg><path/></svg>");
        String secondUri = SvgDataUriUtil.encodeBase64DataUri("<svg><circle/></svg>");
        String source = "const first = \"" + firstUri + "\";\nconst second = \"" + secondUri + "\";";

        List<SvgEmbeddedSearch.Match> matches = SvgEmbeddedSearch.findMatches("/project/icons.js", source);

        assertEquals(2, matches.size());
        assertEquals(1, matches.get(0).getLineNumber());
        assertEquals(2, matches.get(1).getLineNumber());
        assertEquals("/project/icons.js", matches.get(0).getFilePath());
        assertTrue(matches.get(0).getSnippet().contains(firstUri));
    }

    @Test
    public void filtersMatchesByFilePathLineAndSnippet() {
        String uri = SvgDataUriUtil.encodeBase64DataUri("<svg><path/></svg>");
        SvgEmbeddedSearch.Match match = SvgEmbeddedSearch.findMatches(
                "/project/src/icons.js", "const logo = \"" + uri + "\";").get(0);

        assertTrue(SvgEmbeddedSearch.matchesSearch(match, "ICONS.JS"));
        assertTrue(SvgEmbeddedSearch.matchesSearch(match, "logo"));
        assertTrue(SvgEmbeddedSearch.matchesSearch(match, "1"));
        assertFalse(SvgEmbeddedSearch.matchesSearch(match, "missing"));
    }

    @Test
    public void findsUrlEncodedSvgDataUrisWithCharset() {
        String uri = "data:image/svg+xml;charset=UTF-8,"
                + URLEncoder.encode("<svg><path/></svg>", StandardCharsets.UTF_8);

        assertEquals(1, SvgEmbeddedSearch.findMatches(uri).size());
    }

    @Test
    public void ignoresMalformedAndNonSvgDataUris() {
        String invalidSvg = Base64.getEncoder().encodeToString("not svg".getBytes(StandardCharsets.UTF_8));
        String source = "data:image/svg+xml;base64," + invalidSvg
                + " data:image/svg+xml;base64,%%%";

        assertTrue(SvgEmbeddedSearch.findMatches(source).isEmpty());
    }

    @Test
    public void truncatesLongSourceSnippets() {
        String uri = SvgDataUriUtil.encodeBase64DataUri("<svg><path/></svg>");
        String source = "x".repeat(200) + uri;

        String snippet = SvgEmbeddedSearch.findMatches(source).get(0).getSnippet();

        assertEquals(180, snippet.length());
        assertTrue(snippet.endsWith("..."));
    }
}
