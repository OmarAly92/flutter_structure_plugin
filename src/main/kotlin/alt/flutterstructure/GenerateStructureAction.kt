package alt.flutterstructure

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.plugins.terminal.ShellStartupOptions
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.CountDownLatch

class GenerateStructureAction : AnAction("Generate Flutter Structure") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectBase = project.basePath ?: return

        val choice = Messages.showEditableChooseDialog(
            "Choose generation type",
            "Flutter Generator",
            null,
            arrayOf("Generate Full Project", "Generate Feature"),
            "Generate Full Project",
            null
        ) ?: return

        val isFeature = choice == "Generate Feature"
        val dartScriptName = if (isFeature) "generate_feature.dart" else "generate_project.dart"

        var featureName: String? = null
        if (isFeature) {
            featureName = getFeatureNameUI(project)
            if (featureName.isNullOrBlank()) return
        }

        val finalFeatureName = featureName

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Preparing Dart Scripts", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "📁 Copying Dart files..."
                    indicator.fraction = 0.2

                    val toolDir = File(projectBase, "tool")
                    if (!toolDir.exists()) toolDir.mkdirs()

                    val dartFiles = listOf("generate_feature.dart", "generate_project.dart")
                    val classLoader = javaClass.classLoader

                    val copiedFiles = dartFiles.mapNotNull { filename ->
                        val inputStream: InputStream? = classLoader.getResourceAsStream("assets/$filename")
                        if (inputStream != null) {
                            val target = File(toolDir, filename)
                            Files.copy(inputStream, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                            inputStream.close()
                            filename
                        } else null
                    }

                    VfsUtil.markDirtyAndRefresh(true, true, false, toolDir)

                    if (copiedFiles.isEmpty()) {
                        showError(project, "No Dart scripts were copied from plugin resources.")
                        return
                    }

                    indicator.text = "🚀 Preparing to run Dart script..."
                    indicator.fraction = 0.6
                    Thread.sleep(1000)

                    val command = if (isFeature) {
                        "dart run tool/$dartScriptName $finalFeatureName"
                    } else {
                        "dart run tool/$dartScriptName"
                    }

                    runInTerminalSmart(project, command)

                } catch (e: Exception) {
                    showError(project, "Unexpected error: ${e.message}")
                }
            }
        })
    }

    private fun getFeatureNameUI(project: Project): String? {
        var result: String? = null
        val latch = CountDownLatch(1)
        ApplicationManager.getApplication().invokeAndWait {
            result = Messages.showInputDialog(
                project,
                "Enter feature name:",
                "Feature Name",
                Messages.getQuestionIcon()
            )
            latch.countDown()
        }
        latch.await()
        return result
    }

    private fun showError(project: Project, message: String) {
        Messages.showErrorDialog(project, "\u274C $message", "Flutter Structure Generator")
    }

    private fun showNotification(project: Project, title: String, content: String) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("Flutter Structure Generator Group")

        val notification = notificationGroup
            .createNotification(title, content, NotificationType.INFORMATION)

        Notifications.Bus.notify(notification, project)
    }

    private fun runInTerminalSmart(project: Project, command: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val terminalManager = TerminalToolWindowManager.getInstance(project)

                // fallback الآمن – نستخدم الطريقة الأقدم لأنها أكتر توافقاً حاليًا
                @Suppress("DEPRECATION")
                val terminalWidget = terminalManager.createLocalShellWidget(
                    project.basePath ?: ".", "Flutter Generator"
                )

                terminalWidget.executeCommand(command)

                showNotification(project, "✅ Dart command sent to terminal", command)

            } catch (e: Exception) {
                e.printStackTrace()
                showError(project, "❌ Failed to open terminal or send command.\n${e.message}")
            }
        }
    }
}