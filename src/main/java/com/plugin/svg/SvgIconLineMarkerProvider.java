package com.plugin.svg;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindow;
import java.util.regex.Pattern;
import com.plugin.svg.SvgPreviewToolWindowFactory;

/**
 * @author : Pramod Khalkar
 * @since : 24/06/26, Wed
 **/
public class SvgIconLineMarkerProvider implements LineMarkerProvider {

    private static final Logger LOG = Logger.getInstance(SvgIconLineMarkerProvider.class);

    // Broad data URI pattern: capture optional base64 marker and payload (DOTALL to allow newlines)
    private static final Pattern SVG_ICON_PATTERN = Pattern.compile("(?i)data:image/svg\\+xml(?:;charset=[^,;]+)?(?:;(base64))?,(.*)", Pattern.DOTALL);
    private static final int GUTTER_ICON_SIZE = 16; // Size of the gutter icon in pixels
    private static final int PREVIEW_ICON_SIZE = 300; // Size of the preview icon in pixels

    // Shared LRU cache for rendered images
    private static final SvgImageCache imageCache = new SvgImageCache();

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            if (element.getFirstChild() != null) {
                continue;
            }

            LineMarkerInfo<?> info = buildMarker(element);
            if (info != null) {
                result.add(info);
            }
        }
    }

    private String getElementValue(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent != null) {
            // Walk up the tree to find the top-most concatenation expression (Java Polyadic or Binary Expression)
            PsiElement current = parent;
            while (current.getParent() != null) {
                String parentClassName = current.getParent().getClass().getName();
                if (parentClassName.contains("PolyadicExpression") || parentClassName.contains("BinaryExpression")) {
                    current = current.getParent();
                } else {
                    break;
                }
            }

            // Try to evaluate the constant expression using JavaPsiFacade helper via reflection
            try {
                Class<?> javaPsiFacadeClass = Class.forName("com.intellij.psi.JavaPsiFacade");
                Object javaPsiFacade = javaPsiFacadeClass.getMethod("getInstance", com.intellij.openapi.project.Project.class)
                        .invoke(null, element.getProject());
                Object helper = javaPsiFacadeClass.getMethod("getConstantEvaluationHelper").invoke(javaPsiFacade);
                Object value = helper.getClass().getMethod("computeConstantExpression", com.intellij.psi.PsiElement.class)
                        .invoke(helper, current);
                if (value instanceof String) {
                    return (String) value;
                }
            } catch (Exception ignored) {
            }

            // Fallback to calling getValue() on the parent literal (works for both Java and JSON literals)
            try {
                java.lang.reflect.Method getValueMethod = parent.getClass().getMethod("getValue");
                Object val = getValueMethod.invoke(parent);
                if (val instanceof String) {
                    return (String) val;
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback to calling getValue() on element itself
        try {
            java.lang.reflect.Method getValueMethod = element.getClass().getMethod("getValue");
            Object val = getValueMethod.invoke(element);
            if (val instanceof String) {
                return (String) val;
            }
        } catch (Exception ignored) {
        }

        return element.getText();
    }

    private LineMarkerInfo<?> buildMarker(@NotNull PsiElement element) {
        String text = getElementValue(element);
        if (text == null) {
            return null;
        }

        Matcher m = SVG_ICON_PATTERN.matcher(text);
        if (!m.find()) {
            return null;
        }

        String marker = m.group(1); // 'base64' if present
        String payload = m.group(2);
        if (payload == null) {
            return null;
        }
        payload = SvgDataUriUtil.normalizePayload(payload);

        int payloadLength = payload.length();
        String shortId = Integer.toHexString(payload.hashCode());

        // Log at debug level to keep the IDE logs clean
        LOG.debug("[SVG Toolkit] SVG DETECTED: id=" + shortId + " length=" + payloadLength + " base64=" + (marker != null));

        String svgText;
        try {
            LOG.debug("[SVG Toolkit] ATTEMPTING DECODE for id=" + shortId);
            svgText = SvgDataUriUtil.decodePayload(payload, marker != null);
            LOG.debug("[SVG Toolkit] DECODE RETURNED for id=" + shortId + " result=" + (svgText != null ? "NON-NULL" : "NULL"));
        } catch (Throwable t) {
            LOG.warn("[SVG Toolkit] DECODE THREW EXCEPTION for id=" + shortId + ": " + t.getMessage(), t);
            return null;
        }

        if (svgText == null) {
            LOG.warn("[SVG Toolkit] SVG DECODE FAILED: id=" + shortId);
            return null;
        }

        LOG.debug("[SVG Toolkit] SVG DECODED: id=" + shortId + " svg_length=" + svgText.length());

        Project project = element.getProject();

        // create a dynamic icon that can be filled later
        SvgDynamicIcon dynamicIcon = new SvgDynamicIcon(GUTTER_ICON_SIZE, project);

        // Get configured max inline size from settings (using modern API)
        long maxInlineSize = 200000; // default 200KB
        try {
            SvgToolkitSettings settings = ApplicationManager.getApplication().getService(SvgToolkitSettings.class);
            if (settings != null) {
                maxInlineSize = settings.getMaxInlineSizeBytes();
                LOG.debug("[SVG Toolkit] Settings loaded: maxInlineSize=" + maxInlineSize);
            } else {
                LOG.debug("[SVG Toolkit] Settings service returned null, using default 200KB");
            }
        } catch (Throwable e) {
            LOG.warn("[SVG Toolkit] Failed to get settings, using default: " + e.getMessage(), e);
        }

        LOG.debug("[SVG Toolkit] MAX_INLINE_SIZE: " + maxInlineSize + " bytes");

        boolean isLargePayload = svgText.length() > maxInlineSize;

        LOG.debug("[SVG Toolkit] IS_LARGE: " + isLargePayload + " (svg=" + svgText.length() + " vs max=" + maxInlineSize + ")");

        // if payload is small enough, render in background immediately
        if (!isLargePayload) {
            LOG.debug("[SVG Toolkit] RENDERING INLINE for id=" + shortId + ", queuing task...");
            Runnable renderTask = () -> {
                try {
                    LOG.debug("[SVG Toolkit] BACKGROUND THREAD EXECUTED: Starting render for id=" + shortId);
                    BufferedImage img = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
                    LOG.debug("[SVG Toolkit] BACKGROUND THREAD: Render result for id=" + shortId + " = " + (img != null ? "SUCCESS" : "NULL"));
                    if (img != null) {
                        dynamicIcon.setImage(img);
                        LOG.debug("[SVG Toolkit] ICON UPDATED for id=" + shortId);
                    } else {
                        LOG.warn("[SVG Toolkit] RENDER FAILED: null image returned for id=" + shortId);
                    }
                } catch (Exception e) {
                    LOG.warn("[SVG Toolkit] BACKGROUND THREAD ERROR for id=" + shortId + ": " + e.getMessage(), e);
                }
            };
            ApplicationManager.getApplication().executeOnPooledThread(renderTask);
            LOG.debug("[SVG Toolkit] TASK QUEUED for id=" + shortId);
        } else {
            LOG.debug("[SVG Toolkit] LARGE PAYLOAD, will render on demand for id=" + shortId + " (size=" + svgText.length() + ")");
        }

        GutterIconNavigationHandler<PsiElement> clickHandler = ((mouseEvent, psi) -> {
            LOG.info("[SVG Toolkit] CLICK HANDLER INVOKED for id=" + shortId);
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    LOG.info("[SVG Toolkit] CLICK: Rendering preview for id=" + shortId);
                    BufferedImage preview = renderImageFromSvg(svgText, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE);
                    LOG.info("[SVG Toolkit] CLICK: Preview render result = " + (preview != null ? "SUCCESS" : "NULL"));
                    if (preview != null) {
                        if (!dynamicIcon.hasImage()) {
                            BufferedImage gutterImg = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
                            dynamicIcon.setImage(gutterImg);
                        }
                        SwingUtilities.invokeLater(() -> {
                            LOG.info("[SVG Toolkit] CLICK: Showing preview in tool window for id=" + shortId);
                            showPreviewInToolWindow(project, preview, svgText.length());
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    SwingUtilities.getWindowAncestor(
                                            com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)),
                                    "Failed to render SVG preview. The SVG data may be invalid, unsupported, or too large. See IDE logs for details.",
                                    "Preview Error",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } catch (Exception e) {
                    LOG.warn("[SVG Toolkit] CLICK HANDLER ERROR for id=" + shortId + ": " + e.getMessage(), e);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(
                                        com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)),
                                "Error during SVG preview: " + e.getMessage(),
                                "Preview Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        });

        // Tooltip: show cache status and size info
        Supplier<String> tooltipSupplier = () -> {
            if (isLargePayload) {
                return "SVG Preview (large, " + (svgText.length() / 1024) + "KB) - Click to load preview";
            } else {
                return "SVG Preview - Click to open full preview";
            }
        };

        LOG.debug("[SVG Toolkit] MARKER CREATED for id=" + shortId);
        return new LineMarkerInfo<PsiElement>(element,
                element.getTextRange(), dynamicIcon, psi -> tooltipSupplier.get(), clickHandler, GutterIconRenderer.Alignment.LEFT);
    }

    private BufferedImage renderImageFromSvg(String svg, int width, int height) throws Exception {
        return SvgRenderer.render(svg, width, height);
    }

    private void showPreviewInToolWindow(Project project, BufferedImage preview, int svgSizeBytes) {
        SwingUtilities.invokeLater(() -> {
            try {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SVGPreview");
                SvgPreviewToolWindow previewPanel = SvgPreviewToolWindowFactory.getInstance();
                previewPanel.setImageInfo(preview, svgSizeBytes);
                toolWindow.show();
            } catch (Exception e) {
                LOG.warn("[SVG Toolkit] Failed to show preview tool window: " + e.getMessage(), e);
            }
        });
    }
}