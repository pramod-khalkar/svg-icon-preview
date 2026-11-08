package com.plugin.svg.settings;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class SvgToolkitWhatsNewActivity implements StartupActivity.DumbAware {
    private static final String PLUGIN_ID = "com.plugin.svg-icon-preview";

    @Override
    public void runActivity(@NotNull Project project) {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (project.isDisposed() || plugin == null || plugin.getVersion() == null) {
            return;
        }

        String version = plugin.getVersion();
        SvgToolkitSettings settings = ApplicationManager.getApplication().getService(SvgToolkitSettings.class);
        if (!settings.markReleaseNotesShownIfNeeded(version)) {
            return;
        }

        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("SVG Toolkit")
                .createNotification(
                        "SVG Toolkit " + version + " is ready",
                        "See what's new in this version.",
                        NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimple("What's New",
                        () -> new SvgToolkitWhatsNewDialog(project, version).show()));
        notification.notify(project);
    }
}
