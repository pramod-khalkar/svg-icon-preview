package com.plugin.svg.render;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SvgDiagnosticsTest {
    @Test
    public void reportsNoIssuesForSvgWithDimensionsAndInternalReferences() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\">"
                + "<defs><linearGradient id=\"g\"/></defs><rect fill=\"url(#g)\"/><use href=\"#shape\"/>"
                + "</svg>";

        assertTrue(SvgDiagnostics.analyze(source).isEmpty());
    }

    @Test
    public void warnsWhenDimensionsAndViewBoxAreMissing() {
        List<SvgDiagnostics.Issue> issues =
                SvgDiagnostics.analyze("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");

        assertEquals(1, issues.size());
        assertEquals(SvgDiagnostics.Severity.WARNING, issues.get(0).severity());
        assertTrue(issues.get(0).message().contains("width/height or viewBox"));
        assertEquals(1, issues.get(0).line());
    }

    @Test
    public void doesNotWarnWhenViewBoxCanProvideFallbackSizing() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><rect/></svg>";

        assertTrue(SvgDiagnostics.analyze(source).isEmpty());
    }

    @Test
    public void reportsMalformedXmlWithSourceLocation() {
        String source = "<svg width=\"24\" height=\"24\">\n  <g>\n</svg>";

        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze(source);

        assertEquals(1, issues.size());
        assertEquals(SvgDiagnostics.Severity.ERROR, issues.get(0).severity());
        assertEquals(3, issues.get(0).line());
        assertTrue(issues.get(0).column() > 0);
    }

    @Test
    public void warnsAboutExternalResourcesWithoutLoadingExternalDtd() {
        String source = "<!DOCTYPE svg SYSTEM \"https://example.invalid/svg.dtd\">\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\">\n"
                + "  <image href=\"https://example.invalid/icon.png\"/>\n"
                + "</svg>";

        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze(source);

        assertEquals(1, issues.size());
        assertEquals(SvgDiagnostics.Severity.WARNING, issues.get(0).severity());
        assertTrue(issues.get(0).message().contains("external resources"));
        assertEquals(3, issues.get(0).line());
    }

    @Test
    public void reportsNonSvgRootAsAnError() {
        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze("<html/>");

        assertEquals(1, issues.size());
        assertEquals(SvgDiagnostics.Severity.ERROR, issues.get(0).severity());
        assertFalse(issues.get(0).message().isBlank());
    }

    @Test
    public void rendererReturnsStructuredFailureDetails() {
        SvgRenderer.RenderResult result = SvgRenderer.renderWithResult("<svg><g></svg>", 16, 16);

        assertNull(result.image());
        assertFalse(result.errorMessage() == null || result.errorMessage().isBlank());
    }
}
