package com.plugin.svg.render;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author : Pramod Khalkar
 * @since : 24/06/26, Wed
 **/
public class SvgRenderer {

	private SvgRenderer(){}

	public static BufferedImage render(String svgContent, int width, int height) {
		if (svgContent == null) return null;
		RenderResult result = renderWithResult(svgContent, width, height);
		if (result.image() == null) {
			System.err.println("[SVG Toolkit] SVG Rendering failed for content length " + svgContent.length()
					+ ": " + result.errorMessage());
			if (result.cause() != null) {
				result.cause().printStackTrace();
			}
		}
		return result.image();
	}

	public static RenderResult renderWithResult(String svgContent, int width, int height) {
		if (svgContent == null) {
			return new RenderResult(null, "No SVG source was provided.", null);
		}
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(SvgRenderer.class.getClassLoader());

			// Try to clean DOCTYPE declarations that might cause external DTD network lookup failures/hangs
			String cleanedSvg = svgContent.replaceAll("(?s)<!DOCTYPE.*?>", "");
			// If cleaning resulted in empty string, fall back to original
			if (cleanedSvg == null || cleanedSvg.isEmpty()) {
				cleanedSvg = svgContent;
			}

			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float)width);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float)height);
			// Disable DTD validation to prevent external entity loading
			transcoder.addTranscodingHint(PNGTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);

			byte[] svgBytes = cleanedSvg.getBytes(StandardCharsets.UTF_8);
			ByteArrayInputStream in = new ByteArrayInputStream(svgBytes);
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			transcoder.transcode(new TranscoderInput(in), new TranscoderOutput(out));

			BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
			return image == null
					? new RenderResult(null, "The renderer did not produce an image.", null)
					: new RenderResult(image, null, null);
		} catch (Exception e) {
			String message = e.getMessage();
			return new RenderResult(null,
					message == null || message.isBlank() ? "The SVG renderer could not render this document." : message,
					e);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	public record RenderResult(@Nullable BufferedImage image, @Nullable String errorMessage,
							   @Nullable Exception cause) {
	}
}
