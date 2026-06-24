package com.plugin.svg;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

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
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(SvgRenderer.class.getClassLoader());
			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float)width);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float)height);

			byte[] svgBytes = svgContent.getBytes(StandardCharsets.UTF_8);
			ByteArrayInputStream in = new ByteArrayInputStream(svgBytes);
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			transcoder.transcode(new TranscoderInput(in), new TranscoderOutput(out));

			return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
		}catch (Exception e){
			return  null;
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}
}
