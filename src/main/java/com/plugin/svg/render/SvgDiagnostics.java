package com.plugin.svg.render;

import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SvgDiagnostics {
    private static final Pattern CSS_URL_PATTERN =
            Pattern.compile("(?i)url\\(\\s*(['\"]?)(.*?)\\1\\s*\\)");
    private static final Pattern CSS_IMPORT_PATTERN =
            Pattern.compile("(?i)@import\\s+(?:url\\()?\\s*(['\"]?)([^'\";\\s)]+)\\1");
    private static final Pattern PATH_NUMBER_PATTERN =
            Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
    private static final Pattern RENDER_ATTRIBUTE_ERROR_PATTERN = Pattern.compile(
            "(?i)the attribute\\s+[\"']?([\\w:-]+)[\"']?\\s+of the element\\s+<([\\w:-]+)>\\s+is\\s+(.+)");
    private static final Set<String> RESOURCE_ELEMENTS = Set.of(
            "feimage", "image", "lineargradient", "mpath", "pattern", "radialgradient", "script", "textpath", "use");

    private SvgDiagnostics() {
    }

    public static List<Issue> analyze(String source) {
        if (source == null || source.isBlank()) {
            return List.of(new Issue(Severity.ERROR, "No SVG source could be read.", 1, 1));
        }

        AnalyzerHandler handler = new AnalyzerHandler();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(new StringReader(source)));
        } catch (SAXParseException exception) {
            handler.addParseError(exception);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            handler.addError("SVG analysis failed: " + conciseMessage(exception), -1, -1);
        }
        return List.copyOf(handler.issues);
    }

    public static Issue fromRenderError(String renderError) {
        String message = renderError == null || renderError.isBlank()
                ? "The SVG renderer could not render this document."
                : renderError.trim();
        Matcher attributeError = RENDER_ATTRIBUTE_ERROR_PATTERN.matcher(message);
        if (attributeError.matches()) {
            String attribute = attributeError.group(1);
            String element = attributeError.group(2);
            String reason = attributeError.group(3).replaceAll("[.\\s]+$", "");
            String suggestion = reason.toLowerCase(Locale.ROOT).contains("required")
                    ? "Add this required attribute with a valid SVG value for the <" + element + "> element."
                    : "Check the syntax and allowed values for this attribute on <" + element
                    + ">; edit the SVG source and render again to verify.";
            return new Issue(Severity.ERROR, reason, -1, -1, attribute, element, suggestion);
        }

        return new Issue(Severity.ERROR, message, -1, -1, null, null,
                "Inspect the SVG source near the reported renderer failure. Check for unsupported "
                        + "elements, attributes, or values, then edit the source and render again.");
    }

    private static String conciseMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "the XML parser could not analyze this document.";
        }
        String singleLine = message.replaceAll("\\s+", " ").trim();
        return singleLine.length() > 200 ? singleLine.substring(0, 197) + "..." : singleLine;
    }

    private static boolean isExternalReference(String value) {
        String reference = value.trim().toLowerCase(Locale.ROOT);
        return !reference.isEmpty()
                && !reference.startsWith("#")
                && !reference.startsWith("data:")
                && !reference.startsWith("blob:");
    }

    private static String localName(String localName, String qualifiedName) {
        String name = localName == null || localName.isEmpty() ? qualifiedName : localName;
        int separator = name.indexOf(':');
        return (separator >= 0 ? name.substring(separator + 1) : name).toLowerCase(Locale.ROOT);
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    public record Issue(Severity severity, String message, int line, int column,
                        String attribute, String element, String suggestion) {
        public Issue(Severity severity, String message, int line, int column) {
            this(severity, message, line, column, null, null, null);
        }

        public String displayText() {
            if (attribute == null) {
                String location = line > 0
                        ? " (line " + line + (column > 0 ? ", column " + column : "") + ")"
                        : "";
                String text = severity.name() + location + ": " + message;
                return suggestion == null ? text : text + "\nSuggestion: " + suggestion;
            }

            String location = line > 0
                    ? element + " element (line " + line + (column > 0 ? ", column " + column : "") + ")"
                    : element + " element (line/column unavailable)";
            return severity.name()
                    + "\nAttribute: " + attribute
                    + "\nReason: " + message
                    + "\nLocation: " + location
                    + "\nSuggestion: " + suggestion;
        }
    }

    private static final class AnalyzerHandler extends DefaultHandler {
        private final List<Issue> issues = new ArrayList<>();
        private Locator locator;
        private boolean rootSeen;
        private boolean validRoot;
        private boolean hasWidth;
        private boolean hasHeight;
        private boolean hasViewBox;
        private boolean hasExternalReferences;
        private int rootLine = -1;
        private int rootColumn = -1;
        private int externalReferenceLine = -1;
        private int externalReferenceColumn = -1;
        private StringBuilder styleContent;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String qualifiedName, String qName, Attributes attributes) {
            String element = localName(qualifiedName, qName);
            if (!rootSeen) {
                rootSeen = true;
                validRoot = "svg".equals(element);
                rootLine = currentLine();
                rootColumn = currentColumn();
                if (!validRoot) {
                    addError("The document root is not an <svg> element.", rootLine, rootColumn);
                } else {
                    hasWidth = hasAttributeValue(attributes, "width");
                    hasHeight = hasAttributeValue(attributes, "height");
                    hasViewBox = hasAttributeValue(attributes, "viewBox");
                }
            }

            if (!validRoot) {
                return;
            }

            for (int i = 0; i < attributes.getLength(); i++) {
                String attributeName = localName(attributes.getLocalName(i), attributes.getQName(i));
                String value = attributes.getValue(i);
                if (element.equals("path") && attributeName.equals("d")) {
                    inspectPathData(value);
                }
                if (attributeName.equals("style")) {
                    inspectCss(value);
                }
                if (attributeName.equals("href") && RESOURCE_ELEMENTS.contains(element)
                        && isExternalReference(value)) {
                    noteExternalReference();
                }
                inspectCssUrls(value);
            }

            if (element.equals("style")) {
                styleContent = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] characters, int start, int length) {
            if (styleContent != null) {
                styleContent.append(characters, start, length);
            }
        }

        @Override
        public void endElement(String uri, String qualifiedName, String qName) {
            if (styleContent != null && localName(qualifiedName, qName).equals("style")) {
                inspectCss(styleContent.toString());
                styleContent = null;
            }
        }

        @Override
        public void endDocument() {
            if (!validRoot) {
                return;
            }
            if ((!hasWidth || !hasHeight) && !hasViewBox) {
                issues.add(new Issue(Severity.WARNING,
                        "The SVG has no complete width/height or viewBox; its preview may use fallback sizing.",
                        rootLine, rootColumn));
            }
            if (hasExternalReferences) {
                issues.add(new Issue(Severity.WARNING,
                        "This SVG references external resources; they are not fetched for diagnostics and may not appear in the preview.",
                        externalReferenceLine, externalReferenceColumn));
            }
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        private static boolean hasAttributeValue(Attributes attributes, String expectedName) {
            for (int i = 0; i < attributes.getLength(); i++) {
                if (localName(attributes.getLocalName(i), attributes.getQName(i)).equals(expectedName.toLowerCase(Locale.ROOT))
                        && !attributes.getValue(i).isBlank()) {
                    return true;
                }
            }
            return false;
        }

        private void inspectCss(String css) {
            inspectReferences(CSS_URL_PATTERN.matcher(css));
            Matcher imports = CSS_IMPORT_PATTERN.matcher(css);
            while (imports.find()) {
                if (isExternalReference(imports.group(2))) {
                    noteExternalReference();
                }
            }
        }

        private void inspectCssUrls(String value) {
            inspectReferences(CSS_URL_PATTERN.matcher(value));
        }

        private void inspectPathData(String pathData) {
            PathParser parser = new PathParser();
            parser.setPathHandler(DefaultPathHandler.INSTANCE);
            try {
                parser.parse(pathData);
            } catch (ParseException exception) {
                PathErrorContext context = pathErrorContext(pathData, exception.getColumnNumber());
                char command = context == null
                        ? commandAtError(pathData, exception.getColumnNumber())
                        : context.command();
                String suggestion = command == 'A' || command == 'a'
                        ? arcSuggestion(context)
                        : "Check the path command and its required parameters; each command must have the correct number of valid values.";
                issues.add(new Issue(Severity.ERROR, "Invalid SVG path command parameters.",
                        currentLine(), currentColumn(), "d", "Path", suggestion));
            }
        }

        private static String arcSuggestion(PathErrorContext context) {
            String suggestion = "Check the A (arc) command. It requires seven parameters in this order: "
                    + "rx, ry, x-axis rotation, large-arc-flag (0 or 1), sweep-flag (0 or 1), x, and y.";
            if (context != null && context.interruptedBy() != null) {
                suggestion += " The next command '" + context.interruptedBy()
                        + "' appears before the arc is complete. Add the missing values, "
                        + "or replace 'A' if it was not intended to start an arc.";
            } else {
                suggestion += " Complete the arc parameters or replace 'A' if it was not intended to start an arc.";
            }
            return suggestion;
        }

        private static PathErrorContext pathErrorContext(String pathData, int errorColumn) {
            int limit = errorColumn > 0
                    ? Math.min(errorColumn, pathData.length())
                    : pathData.length();
            char activeCommand = '\0';
            int parameterCount = 0;
            Matcher number = PATH_NUMBER_PATTERN.matcher(pathData);
            for (int index = 0; index < limit; index++) {
                char character = pathData.charAt(index);
                if (isPathCommand(character)) {
                    if ((activeCommand == 'A' || activeCommand == 'a') && parameterCount % 7 != 0) {
                        return new PathErrorContext(activeCommand, character);
                    }
                    activeCommand = character;
                    parameterCount = 0;
                    continue;
                }
                if (activeCommand == '\0') {
                    continue;
                }
                number.region(index, limit);
                if (number.lookingAt()) {
                    parameterCount++;
                    index = number.end() - 1;
                }
            }
            if ((activeCommand == 'A' || activeCommand == 'a') && parameterCount % 7 != 0) {
                return new PathErrorContext(activeCommand, null);
            }
            return null;
        }

        private static boolean isPathCommand(char character) {
            return "MmZzLlHhVvCcSsQqTtAa".indexOf(character) >= 0;
        }

        private static char commandAtError(String pathData, int errorColumn) {
            int index = errorColumn > 0
                    ? Math.min(errorColumn - 1, pathData.length() - 1)
                    : pathData.length() - 1;
            for (int i = index; i >= 0; i--) {
                char candidate = pathData.charAt(i);
                if ("MmZzLlHhVvCcSsQqTtAa".indexOf(candidate) >= 0) {
                    return candidate;
                }
            }
            return '\0';
        }

        private record PathErrorContext(char command, Character interruptedBy) {
        }

        private void inspectReferences(Matcher references) {
            while (references.find()) {
                if (isExternalReference(references.group(2))) {
                    noteExternalReference();
                }
            }
        }

        private void noteExternalReference() {
            if (!hasExternalReferences) {
                externalReferenceLine = currentLine();
                externalReferenceColumn = currentColumn();
            }
            hasExternalReferences = true;
        }

        private int currentLine() {
            return locator == null ? -1 : locator.getLineNumber();
        }

        private int currentColumn() {
            return locator == null ? -1 : locator.getColumnNumber();
        }

        private void addParseError(SAXParseException exception) {
            addError("Invalid SVG/XML: " + conciseMessage(exception),
                    exception.getLineNumber(), exception.getColumnNumber());
        }

        private void addError(String message, int line, int column) {
            issues.add(new Issue(Severity.ERROR, message, line, column));
        }
    }
}
