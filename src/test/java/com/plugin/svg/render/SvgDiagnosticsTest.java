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
    public void reportsMalformedPathDataWithArcParameterSuggestionAndLocation() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\">\n"
                + "  <path d=\"M 0 0 A 10 10 0 0 1 20\"/>\n"
                + "</svg>";

        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze(source);

        assertEquals(1, issues.size());
        SvgDiagnostics.Issue issue = issues.get(0);
        assertEquals(SvgDiagnostics.Severity.ERROR, issue.severity());
        assertEquals("d", issue.attribute());
        assertEquals(2, issue.line());
        assertTrue(issue.column() > 0);
        assertTrue(issue.displayText().contains("Attribute: d"));
        assertTrue(issue.displayText().contains("Reason: Invalid SVG path command parameters."));
        assertTrue(issue.displayText().contains("Location: Path element (line 2"));
        assertTrue(issue.displayText().contains("A (arc) command"));
        assertTrue(issue.displayText().contains("requires seven parameters"));
    }

    @Test
    public void malformedNonArcPathGetsGeneralParameterSuggestion() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\">"
                + "<path d=\"M 0 0 L\"/></svg>";

        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze(source);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).suggestion().contains("correct number"));
        assertFalse(issues.get(0).suggestion().contains("A (arc)"));
    }

    @Test
    public void identifiesIncompleteArcWhenNextPathCommandInterruptsIt() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">\n"
                + "  <path d=\"M12 2l4.25 10.58H1A3h10.58L16 20l1.42 4.58L12 20l-4.42 4.58L12 20l-4.67-3.42z\" fill=\"#FFB300\"/>\n"
                + "</svg>";

        List<SvgDiagnostics.Issue> issues = SvgDiagnostics.analyze(source);

        assertEquals(1, issues.size());
        SvgDiagnostics.Issue issue = issues.get(0);
        assertEquals("d", issue.attribute());
        assertTrue(issue.suggestion(), issue.suggestion().contains("A (arc) command"));
        assertTrue(issue.suggestion(), issue.suggestion().contains("seven parameters"));
        assertTrue(issue.suggestion(), issue.suggestion().contains("h"));
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
    public void turnsRendererAttributeErrorsIntoStructuredEditingGuidance() {
        SvgDiagnostics.Issue issue =
                SvgDiagnostics.fromRenderError("The attribute \"offset\" of the element <stop> is required");

        assertEquals("offset", issue.attribute());
        assertEquals("stop", issue.element());
        assertEquals("required", issue.message());
        assertEquals(-1, issue.line());
        assertTrue(issue.suggestion().contains("Add this required attribute"));
        assertTrue(issue.displayText().contains("Attribute: offset"));
        assertTrue(issue.displayText().contains("Location: stop element (line/column unavailable)"));
        assertTrue(issue.displayText().contains("Suggestion:"));
    }

    @Test
    public void keepsUnrecognizedRendererErrorsAndProvidesGeneralGuidance() {
        SvgDiagnostics.Issue issue = SvgDiagnostics.fromRenderError("Unsupported SVG feature");

        assertEquals("Unsupported SVG feature", issue.message());
        assertTrue(issue.displayText().contains("Suggestion: Inspect the SVG source"));
    }

    @Test
    public void rendererReturnsStructuredFailureDetails() {
        SvgRenderer.RenderResult result = SvgRenderer.renderWithResult("<svg><g></svg>", 16, 16);

        assertNull(result.image());
        assertFalse(result.errorMessage() == null || result.errorMessage().isBlank());
    }

    @Test
    public void rendererReportsBatikPathAttributeFailureInsteadOfNull() {
        String source = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\">"
                + "<path d=\"M 0 0 L\"/></svg>";

        SvgRenderer.RenderResult result = SvgRenderer.renderWithResult(source, 16, 16);

        assertNull(result.image());
        assertTrue(result.errorMessage(), result.errorMessage().contains("attribute \"d\""));
    }

}
