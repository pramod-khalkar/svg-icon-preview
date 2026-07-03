package com.plugin.svg;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
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
import java.util.regex.Pattern;
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
		for(PsiElement element : elements) {
			if(element.getFirstChild() != null ) continue;

			LineMarkerInfo<?> info = buildMarker(element);
			if(info != null) {
				result.add(info);
			}
		}
	}

	private LineMarkerInfo<?> buildMarker(@NotNull PsiElement element){
		String text =  element.getText();
		if(text == null) return null;

		Matcher m = SVG_ICON_PATTERN.matcher(text);
		if(!m.find()) return null;

		String marker = m.group(1); // 'base64' if present
		String payload = m.group(2);
		if (payload == null) return null;
		payload = SvgDataUriUtil.normalizePayload(payload);

		int payloadLength = payload.length();
		String shortId = Integer.toHexString(payload.hashCode());
		
		// Log at ERROR level to ensure it shows up
		LOG.error("SVG DETECTED: id=" + shortId + " length=" + payloadLength + " base64=" + (marker!=null));

		String svgText;
		try {
			LOG.error("ATTEMPTING DECODE for id=" + shortId);
			svgText = SvgDataUriUtil.decodePayload(payload, marker != null);
			LOG.error("DECODE RETURNED for id=" + shortId + " result=" + (svgText != null ? "NON-NULL" : "NULL"));
		} catch (Throwable t) {
			LOG.error("DECODE THREW EXCEPTION for id=" + shortId + ": " + t.getMessage(), t);
			return null;
		}
		
		if (svgText == null) {
			LOG.error("SVG DECODE FAILED: id=" + shortId);
			return null;
		}

		LOG.error("SVG DECODED: id=" + shortId + " svg_length=" + svgText.length());

		// create a dynamic icon that can be filled later
		SvgDynamicIcon dynamicIcon = new SvgDynamicIcon(GUTTER_ICON_SIZE, element.getProject());

		// Get configured max inline size from settings (using modern API)
		long maxInlineSize = 200000; // default 200KB
		try {
			SvgToolkitSettings settings = ApplicationManager.getApplication().getService(SvgToolkitSettings.class);
			if (settings != null) {
				maxInlineSize = settings.getMaxInlineSizeBytes();
				LOG.error("Settings loaded: maxInlineSize=" + maxInlineSize);
			} else {
				LOG.error("Settings service returned null, using default 200KB");
			}
		} catch (Throwable e) {
			LOG.error("Failed to get settings, using default: " + e.getMessage(), e);
		}
		
		LOG.error("MAX_INLINE_SIZE: " + maxInlineSize + " bytes");

		boolean isLargePayload = svgText.length() > maxInlineSize;

		LOG.error("IS_LARGE: " + isLargePayload + " (svg=" + svgText.length() + " vs max=" + maxInlineSize + ")");

		// if payload is small enough, render in background immediately
		if (!isLargePayload) {
			LOG.error("RENDERING INLINE for id=" + shortId + ", queuing task...");
			Runnable renderTask = () -> {
				try {
					LOG.error("BACKGROUND THREAD EXECUTED: Starting render for id=" + shortId);
					BufferedImage img = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
					LOG.error("BACKGROUND THREAD: Render result for id=" + shortId + " = " + (img != null ? "SUCCESS" : "NULL"));
					if (img != null) {
						dynamicIcon.setImage(img);
						LOG.error("ICON UPDATED for id=" + shortId);
					} else {
						LOG.error("RENDER FAILED: null image returned for id=" + shortId);
					}
				} catch (Throwable t) {
					LOG.error("BACKGROUND THREAD ERROR for id=" + shortId + ": " + t.getMessage(), t);
				}
			};
			ApplicationManager.getApplication().executeOnPooledThread(renderTask);
			LOG.error("TASK QUEUED for id=" + shortId);
		} else {
			LOG.error("LARGE PAYLOAD, will render on demand for id=" + shortId + " (size=" + svgText.length() + ")");
		}

		GutterIconNavigationHandler<PsiElement> clickHandler = ((mouseEvent, psi) -> {
			LOG.error("CLICK HANDLER INVOKED for id=" + shortId);
			ApplicationManager.getApplication().executeOnPooledThread(() -> {
				try {
					LOG.error("CLICK: Rendering preview for id=" + shortId);
					BufferedImage preview = renderImageFromSvg(svgText, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE);
					LOG.error("CLICK: Preview render result = " + (preview != null ? "SUCCESS" : "NULL"));
					if(preview != null){
						if (!dynamicIcon.hasImage() && !isLargePayload) {
							BufferedImage gutterImg = renderImageFromSvg(svgText, GUTTER_ICON_SIZE, GUTTER_ICON_SIZE);
							dynamicIcon.setImage(gutterImg);
						}
						SwingUtilities.invokeLater(() -> {
							LOG.error("CLICK: Opening preview dialog for id=" + shortId);
							showPreviewDialog(preview);
						});
					}
				} catch (Throwable t) {
					LOG.error("CLICK HANDLER ERROR for id=" + shortId + ": " + t.getMessage(), t);
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

		LOG.error("MARKER CREATED for id=" + shortId);
		return new LineMarkerInfo<PsiElement>(element,
				element.getTextRange(), dynamicIcon, psi -> tooltipSupplier.get(), clickHandler, GutterIconRenderer.Alignment.LEFT);
	}

	@Nullable
	private BufferedImage renderImageFromSvg(String svg, int width, int height){
		try {
			return SvgRenderer.render(svg, width, height);
		} catch (Exception e) {
			LOG.error("Render failed: " + e.getMessage(), e);
			return null;
		}
	}

	private void showPreviewDialog(BufferedImage img){
		SvgPreviewDialog dialog = new SvgPreviewDialog(null, img);
		dialog.setVisible(true);
	}
}
