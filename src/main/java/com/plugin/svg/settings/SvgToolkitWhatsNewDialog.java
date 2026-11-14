package com.plugin.svg.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

final class SvgToolkitWhatsNewDialog extends DialogWrapper {
    private final String version;

    SvgToolkitWhatsNewDialog(Project project, String version) {
        super(project);
        this.version = version;
        setTitle("What's New in SVG Toolkit " + version);
        setModal(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JEditorPane releaseNotes = new JEditorPane("text/html", getReleaseNotesHtml());
        releaseNotes.setEditable(false);
        releaseNotes.setOpaque(false);
        releaseNotes.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(releaseNotes), BorderLayout.CENTER);
        panel.setPreferredSize(new java.awt.Dimension(480, 300));
        return panel;
    }

    private String getReleaseNotesHtml() {
        return "<html><body style='font-family: sans-serif;'>" +
                "<p>Highlights in SVG Toolkit " + version + ":</p>" +
                "<ul>" +
                "<li>Review malformed SVG/XML and rendering errors in the preview, with source locations when available.</li>" +
                "<li>Get structured SVG path-data errors with command-specific repair suggestions.</li>" +
                "<li>See non-blocking warnings when intrinsic sizing is incomplete or an SVG references external resources.</li>" +
                "<li>Expand diagnostic details and double-click a located issue to jump to its source.</li>" +
                "<li>Open an SVG in the editor even when gutter or embedded-result rendering fails, then inspect the renderer diagnostics.</li>" +
                "<li>Diagnostics do not fetch external resources.</li>" +
                "</ul>" +
                "<p>Open an SVG preview to see its diagnostics. " +
                "The next time SVG Toolkit is updated, you'll see this notification again.</p>" +
                "</body></html>";
    }
}
