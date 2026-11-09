package com.plugin.svg.editor;

import org.jetbrains.annotations.Nullable;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SvgDimensions {
    private static final Pattern SVG_ROOT_PATTERN =
            Pattern.compile("<(?:[\\w.-]+:)?svg\\b([^>]*)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SVG_WIDTH_PATTERN = svgAttributePattern("width");
    private static final Pattern SVG_HEIGHT_PATTERN = svgAttributePattern("height");
    private static final Pattern SVG_VIEWBOX_PATTERN = svgAttributePattern("viewBox");
    private static final Pattern SVG_LENGTH_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)(px|in|cm|mm|pt|pc)?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private SvgDimensions() {
    }

    public static String formatImageDetails(BufferedImage image, @Nullable String source) {
        double[] dimensions = getDimensions(source);
        double width = dimensions == null ? image.getWidth() : dimensions[0];
        double height = dimensions == null ? image.getHeight() : dimensions[1];
        int sourceBytes = source == null ? 0 : source.getBytes(StandardCharsets.UTF_8).length;
        return String.format(Locale.ROOT, "%s × %s px · SVG %s",
                formatDimension(width), formatDimension(height), formatByteSize(sourceBytes));
    }

    public static @Nullable String formatSvgDimensions(@Nullable String source) {
        double[] dimensions = getDimensions(source);
        if (dimensions == null) {
            return null;
        }
        return formatDimension(dimensions[0]) + " × " + formatDimension(dimensions[1]) + " px";
    }

    public static Dimension getRasterDimensions(@Nullable String source, int maxDimension) {
        if (maxDimension < 1) {
            throw new IllegalArgumentException("Maximum raster dimension must be positive");
        }
        double[] dimensions = getDimensions(source);
        if (dimensions == null) {
            return new Dimension(maxDimension, maxDimension);
        }
        double scale = maxDimension / Math.max(dimensions[0], dimensions[1]);
        int width = Math.max(1, (int) Math.round(dimensions[0] * scale));
        int height = Math.max(1, (int) Math.round(dimensions[1] * scale));
        return new Dimension(width, height);
    }

    private static double[] getDimensions(@Nullable String source) {
        if (source == null) {
            return null;
        }
        Matcher rootMatcher = SVG_ROOT_PATTERN.matcher(source);
        if (!rootMatcher.find()) {
            return null;
        }

        String attributes = rootMatcher.group(1);
        double width = parseSvgLength(getSvgAttribute(attributes, SVG_WIDTH_PATTERN));
        double height = parseSvgLength(getSvgAttribute(attributes, SVG_HEIGHT_PATTERN));
        double[] viewBox = parseViewBox(getSvgAttribute(attributes, SVG_VIEWBOX_PATTERN));

        if (width <= 0 && height <= 0 && viewBox != null) {
            width = viewBox[2];
            height = viewBox[3];
        } else if (width > 0 && height <= 0 && viewBox != null) {
            height = width * viewBox[3] / viewBox[2];
        } else if (height > 0 && width <= 0 && viewBox != null) {
            width = height * viewBox[2] / viewBox[3];
        }

        return width > 0 && height > 0 && Double.isFinite(width) && Double.isFinite(height)
                ? new double[]{width, height}
                : null;
    }

    private static Pattern svgAttributePattern(String name) {
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(name) + "\\s*=\\s*(['\"])(.*?)\\1",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    private static String getSvgAttribute(String attributes, Pattern pattern) {
        Matcher matcher = pattern.matcher(attributes);
        return matcher.find() ? matcher.group(2) : null;
    }

    private static double parseSvgLength(@Nullable String value) {
        if (value == null) {
            return -1;
        }
        Matcher matcher = SVG_LENGTH_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return -1;
        }
        double length = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null || unit.equalsIgnoreCase("px")) return length;
        if (unit.equalsIgnoreCase("in")) return length * 96;
        if (unit.equalsIgnoreCase("cm")) return length * (96 / 2.54);
        if (unit.equalsIgnoreCase("mm")) return length * (96 / 25.4);
        if (unit.equalsIgnoreCase("pt")) return length * (96 / 72.0);
        if (unit.equalsIgnoreCase("pc")) return length * 16;
        return -1;
    }

    private static double[] parseViewBox(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split("[,\\s]+");
        if (parts.length != 4) {
            return null;
        }
        try {
            double width = Double.parseDouble(parts[2]);
            double height = Double.parseDouble(parts[3]);
            return width > 0 && height > 0 ? new double[]{Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]), width, height} : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatDimension(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String formatByteSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024L) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
