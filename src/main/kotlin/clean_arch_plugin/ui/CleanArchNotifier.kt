/*
 * Copyright: Copyright (c) 2019 Arne Rantzen <arne@rantzen.net>
 * License: GPL-3
 * Last Edited: 07.12.19, 22:29
 */

package clean_arch_plugin.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

/**
 * Notification factory
 */
interface CleanArchNotifier {
    companion object {
        /**
         * Creates a warning notification in the [project] context with the [content]
         */
        fun warning(project: Project?, content: String) =
            Notifications.Bus.notify(
                Notification(
                    "CleanArchitectureGenerator",
                    "Clean-Architecture CleanArchGenerator Warning",
                    content,
                    NotificationType.WARNING
                ), project
            )

        /**
         * Creates an error notification in the [project] context with the [content]
         */
        fun error(project: Project?, content: String) =
            Notifications.Bus.notify(
                Notification(
                    "CleanArchitectureGenerator",
                    "Clean-Architecture CleanArchGenerator Error",
                    content,
                    NotificationType.ERROR
                ), project
            )
    }
}
