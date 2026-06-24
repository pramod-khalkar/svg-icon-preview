package com.plugin.svg;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author : Pramod Khalkar
 * @since : 24/06/26, Wed
 **/
public class SvgIconLineMarkerProvider  implements LineMarkerProvider {

	private static final Pattern SVG_ICON_PATTERN = Pattern.compile("data:image/svg\\+xml;base64,([A-Za-z0-9+/\\r\\n]+=*)");
	private static final int GUTTER_ICON_SIZE = 16; // Size of the gutter icon in pixels
	private static final int PREVIEW_ICON_SIZE = 300; // Size of the preview icon in pixels

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

		String base64Part = m.group(1).replaceAll("\\s+", ""); // Remove whitespace and newlines
		Icon  gutterIcon = renderIcon(base64Part, GUTTER_ICON_SIZE);
		if(gutterIcon == null) return null;

		GutterIconNavigationHandler<PsiElement> clickHandler = ((mouseEvent, psi) -> {
			ApplicationManager.getApplication().executeOnPooledThread(() -> {
				BufferedImage preview = renderImage(base64Part, PREVIEW_ICON_SIZE,PREVIEW_ICON_SIZE);
				if(preview != null){
					SwingUtilities.invokeLater(() -> showPreviewDialog(preview));
				}
			});
		});

		return new LineMarkerInfo<>(element,
				element.getTextRange(),gutterIcon,psi -> "SVG Preview - click to open full preview",clickHandler, GutterIconRenderer.Alignment.LEFT,() -> "SVG Preview");
	}

	private void showPreviewDialog(BufferedImage img){
		JDialog dialog = new JDialog((Frame)null, "SVG Preview", false);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JLabel label = new JLabel(new ImageIcon(img));
		label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		dialog.getContentPane().add(label);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	@Nullable
	private Icon renderIcon(String base64, int size){
		BufferedImage img = renderImage(base64, size, size);
		if(img == null) return null;
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.drawImage(img, x, y, size,size,null);
			}

			@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}
		};
	}

	private BufferedImage renderImage(String base64, int width, int height){
		try {
			byte[] svgBytes = java.util.Base64.getDecoder().decode(base64);
			String svgContent = new String(svgBytes,java.nio.charset.StandardCharsets.UTF_8);
			return SvgRenderer.render(svgContent,width,height);
		} catch (Exception e) {
			return null;
		}
	}
}
