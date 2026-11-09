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
                "<li>Browse standalone SVGs with thumbnails, dimensions, file sizes, and project-relative paths.</li>" +
                "<li>Search project files for SVG data URIs and inspect thumbnails, source snippets, and line numbers.</li>" +
                "<li>Preview search results or jump directly to their source locations.</li>" +
                "<li>Edit standalone SVGs, save changes to the original, or export SVG and PNG files.</li>" +
                "<li>Select multiple standalone SVGs or embedded data URI matches and batch-export them into one directory at multiple sizes, with collision-safe filenames and per-file or batch-wide existing-file choices.</li>" +
                "<li>Reopening the same SVG reuses its existing SVG Toolkit preview tab.</li>" +
                "</ul>" +
                "<p>Open <b>View → Tool Windows → SVG Toolkit</b> to explore the browser. " +
                "The next time SVG Toolkit is updated, you'll see this notification again.</p>" +
                "</body></html>";
    }
}
