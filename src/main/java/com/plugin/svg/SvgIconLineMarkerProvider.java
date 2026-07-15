package com.plugin.svg;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects base64 (or url-encoded) SVG data URIs in any text-based file and shows a single
 * gutter icon per line, regardless of the file's language/type (JSON, YAML, plain text, etc.).
 *
 * Design notes (see history of this file for why this approach was chosen):
 *  - Only {@link #collectSlowLineMarkers} produces markers. {@link #getLineMarkerInfo} always
 *    returns null so there is exactly one code path that can ever add a marker - this is what
 *    prevents duplicate icons on the same line.
 *  - Only PSI *leaf* elements (no children) are inspected. For structured languages (JSON, YAML,
 *    Java, ...) the leaf token already contains the full raw source text of a string/value, so
 *    scanning it directly is enough - no fragile "value extraction"/reflection is needed. For
 *    plain text files, the entire file content is typically a single leaf, so the same leaf-scan
 *    approach works there too. This avoids the historical bug of also matching on composite
 *    parent PSI nodes (which caused 2-3 icons per line for JSON/YAML).
 *  - Deduplication of markers is done with a plain local (non-static) Set scoped to a single
 *    {@link #collectSlowLineMarkers} invocation, keyed by file + line number. Using a static map
 *    across calls previously caused stale-state bugs (icons disappearing or blocked forever).
 */
public class SvgIconLineMarkerProvider implements LineMarkerProvider {

    private static final Logger LOG = Logger.getInstance(SvgIconLineMarkerProvider.class);

    private static final String SVG_PREFIX = "data:image/svg";

    private static final Pattern SVG_ICON_PATTERN = Pattern.compile(
            "(?i)data:image/svg\\+xml(?:;charset=[^,;]+)?(?:;(base64))?,([A-Za-z0-9+/=_%\\-\\s]+)"
    );

    private static final int GUTTER_ICON_SIZE = 16;
    private static final int PREVIEW_ICON_SIZE = 300;
    private static final SvgImageCache imageCache = new SvgImageCache();

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        // Intentionally always null. All marker creation happens in collectSlowLineMarkers so
        // that there is a single, deterministic path producing at most one marker per line.
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                        @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) {
            return;
        }

        ReadAction.run(() -> {
            // Scoped to this single pass only - guarantees at most one marker per (file, line)
            // for this call, without leaking state across highlighting passes.
            Set<String> seenLineKeys = new HashSet<>();

            for (PsiElement element : elements) {
                // Only inspect leaf PSI nodes. Composite parents wrapping the same source text
                // (e.g. a JSON/YAML value expression wrapping its literal token) would otherwise
                // be scanned twice for the same text, producing duplicate icons.
                if (element.getFirstChild() != null) {
                    continue;
                }

                processLeaf(element, seenLineKeys, result);
            }
        });
    }

    private void processLeaf(@NotNull PsiElement element,
                              @NotNull Set<String> seenLineKeys,
                              @NotNull Collection<? super LineMarkerInfo<?>> result) {
        PsiFile file = element.getContainingFile();
        if (file == null || file.getVirtualFile() == null) {
            return;
        }
        if (!file.getViewProvider().isPhysical() || file != file.getOriginalFile()) {
            return;
        }

        String elementText = element.getText();
        if (elementText == null || elementText.isEmpty()
                || elementText.toLowerCase(Locale.ROOT).indexOf(SVG_PREFIX) < 0) {
            return;
        }

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        int elementStart = element.getTextRange().getStartOffset();
        String filePath = file.getVirtualFile().getPath();

        Matcher matcher = SVG_ICON_PATTERN.matcher(elementText);
        while (matcher.find()) {
            String marker = matcher.group(1);
            String payload = trimPayloadForAnyFile(matcher.group(2));
            if (payload == null || payload.isEmpty()) {
                continue;
            }

            int matchStartInDoc = elementStart + matcher.start();
            int matchEndInDoc = elementStart + matcher.end();

            int lineNumber = 0;
            if (document != null && document.getTextLength() > 0) {
                int safeOffset = Math.max(0, Math.min(matchStartInDoc, document.getTextLength() - 1));
                lineNumber = document.getLineNumber(safeOffset);
            }

            String lineKey = filePath + "#" + lineNumber;
            if (!seenLineKeys.add(lineKey)) {
                // Already have a marker for this line in this pass.
                continue;
            }

            TextRange markerRange = new TextRange(matchStartInDoc, Math.min(matchStartInDoc + 1, matchEndInDoc));
            String sourceText = matcher.group(0);
            LineMarkerInfo<?> info = buildMarker(element, markerRange, marker, payload, sourceText);
            if (info != null) {
                result.add(info);
            }
        }
    }

    private @Nullable String trimPayloadForAnyFile(@Nullable String payload) {
        if (payload == null) {
            return null;
        }
        String normalized = SvgDataUriUtil.normalizePayload(payload);
        if (normalized == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '+' || c == '/' || c == '=' || c == '_' || c == '-' || c == '%') {
                sb.append(c);
                started = true;
            } else if (started && !Character.isWhitespace(c)) {
                break;
            }
        }

        String cleaned = sb.toString().trim();
        return cleaned.isEmpty() ? normalized : cleaned;
    }

    private @Nullable LineMarkerInfo<?> buildMarker(@NotNull PsiElement element,
                                                     @NotNull TextRange markerRange,
                                                     @Nullable String marker,
                                                     @NotNull String payload,
                                                     @NotNull String sourceText) {
        String shortId = Integer.toHexString(payload.hashCode());

        String svgText;
        try {
            svgText = SvgDataUriUtil.decodePayload(payload, marker != null);
        } catch (Throwable t) {
            LOG.warn("[SVG Toolkit] Decode failed for id=" + shortId + ": " + t.getMessage(), t);
            return null;
        }

        if (svgText == null) {
            return null;
        }

        Project project = element.getProject();
        SvgDynamicIcon dynamicIcon = new SvgDynamicIcon(GUTTER_ICON_SIZE, project);
        BufferedImage cached = imageCache.get(shortId);
        if (cached != null) {
            dynamicIcon.setImage(cached);
        }

        long maxInlineSize = 200000;
        try {
            SvgToolkitSettings settings = ApplicationManager.getApplication().getService(SvgToolkitSettings.class);
            if (settings != null) {
                maxInlineSize = settings.getMaxInlineSizeBytes();
            }
        } catch (Throwable e) {
            LOG.warn("[SVG Toolkit] Failed to read settings: " + e.getMessage(), e);
        }

        boolean isLargePayload = svgText.length() > maxInlineSize;
        if (!isLargePayload && cached == null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    BufferedImage img = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
                    if (img != null) {
                        imageCache.put(shortId, img);
                        dynamicIcon.setImage(img);
                    }
                } catch (Exception e) {
                    LOG.warn("[SVG Toolkit] Gutter render failed for id=" + shortId + ": " + e.getMessage(), e);
                }
            });
        }

        GutterIconNavigationHandler<PsiElement> clickHandler = (mouseEvent, psi) -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                BufferedImage preview = renderImageFromSvg(svgText, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE);
                if (preview == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)),
                            "Failed to render SVG preview.",
                            "Preview Error",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }

                if (!dynamicIcon.hasImage()) {
                    BufferedImage gutterImg = imageCache.get(shortId);
                    if (gutterImg == null) {
                        gutterImg = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
                        imageCache.put(shortId, gutterImg);
                    }
                    dynamicIcon.setImage(gutterImg);
                }

                SwingUtilities.invokeLater(() -> {
                    String tempFileName = "svg-preview-" + System.currentTimeMillis();
                    SvgVirtualFile virtualFile = new SvgVirtualFile(tempFileName, svgText, sourceText, preview);
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                });
            } catch (Exception e) {
                LOG.warn("[SVG Toolkit] Preview failed for id=" + shortId + ": " + e.getMessage(), e);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)),
                        "Error during SVG preview: " + e.getMessage(),
                        "Preview Error",
                        JOptionPane.ERROR_MESSAGE));
            }
        });

        Supplier<String> tooltipSupplier = () -> isLargePayload
                ? "SVG Preview (large, " + (svgText.length() / 1024) + "KB) - Click to load preview"
                : "SVG Preview - Click to open full preview";

        return new LineMarkerInfo<>(element, markerRange, dynamicIcon, psi -> tooltipSupplier.get(), clickHandler, GutterIconRenderer.Alignment.LEFT);
    }

    private BufferedImage renderImageFromSvg(String svg, int width, int height) throws Exception {
        return SvgRenderer.render(svg, width, height);
    }
}
