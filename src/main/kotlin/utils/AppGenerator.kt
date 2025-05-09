package utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import screen_plugin.ui.ScreenNotifier
import java.io.IOException
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipInputStream

class AppGenerator {
    companion object {
        /**
         * Creates a [parent] folder and its [children] in a given [folder].
         * [project] is needed for the notifications if there is an error or a warning situation.
         * @return null if an error occurred or the a map of all virtual files created
         */
        fun createFolder(
            project: Project,
            folder: VirtualFile,
            parent: String,
            vararg children: String
        ): Map<String, VirtualFile>? {
            try {
                for (child in folder.children) {
                    if (child.name == parent) {
                        ScreenNotifier.warning(project, "Directory [$parent] already exists")
                        return null
                    }
                }
                val mapOfFolder = mutableMapOf<String, VirtualFile>()
                mapOfFolder[parent] = folder.createChildDirectory(folder, parent)
                for (child in children) {
                    mapOfFolder[child] =
                        mapOfFolder[parent]?.createChildDirectory(mapOfFolder[parent], child)
                            ?: throw IOException()
                }
                return mapOfFolder
            } catch (e: IOException) {
                ScreenNotifier.warning(project, "Couldn't create $parent directory")
                e.printStackTrace()
                return null
            }
        }

        fun getFlutterPackageName(projectDir: VirtualFile): String? {
            val pubspec = projectDir.findChild("pubspec.yaml") ?: return null
            val text = VfsUtil.loadText(pubspec)
            val nameLine = text.lineSequence().firstOrNull { it.trim().startsWith("name:") }
            return nameLine?.split(":")?.getOrNull(1)?.trim()
        }

        fun createDartFile(
            directory: VirtualFile,
            fileName: String,
            content: String
        ) {
            val dartFileName = "$fileName.dart"
            val dartFile = directory.findOrCreateChildData(this, dartFileName)
            try {
                VfsUtil.saveText(dartFile, content)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun snakeToCamelCase(snake: String): String {
            return snake.split('_')
                .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        fun convertToSnakeCase(input: String): String {
            return input.fold(StringBuilder()) { acc, c ->
                when {
                    c.isUpperCase() -> {
                        if (acc.isNotEmpty()) acc.append('_')
                        acc.append(c.lowercaseChar())
                    }

                    else -> acc.append(c)
                }
            }.toString()
        }

        fun runInTerminalSmart(project: Project, command: String, retry: Boolean = true) {
            ApplicationManager.getApplication().invokeLater {
                try {
                    val terminalManager = TerminalToolWindowManager.getInstance(project)
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

        private fun showNotification(project: Project, title: String, content: String) {
            val notificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("Flutter Structure Generator Group")

            val notification = notificationGroup
                .createNotification(title, content, NotificationType.INFORMATION)

            Notifications.Bus.notify(notification, project)
        }

        private fun showError(project: Project, message: String) {
            Messages.showErrorDialog(project, "\u274C $message", "Flutter Structure Generator")
        }

        private fun generateFromTemplate(
            brickPath: String,
            outputPath: String,
            vars: Map<String, String>? = null
        ) {
            val inputDir = File(brickPath)
            val outputDir = File(outputPath)

            if (!inputDir.exists() || !inputDir.isDirectory) {
                throw Exception("Brick path does not exist: $brickPath")
            }

            inputDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(inputDir).path
                    val parsedPath = replaceVars(relativePath, vars)
                    val outputFile = File(outputDir, parsedPath)

                    outputFile.parentFile.mkdirs()
                    var content = file.readText()
                    content = replaceVars(content, vars)
                    outputFile.writeText(content)
                }
            }
        }

        private fun replaceVars(input: String, vars: Map<String, String>?): String {
            val regex = Regex("""\{\{(\w+)\}\}""")
            return regex.replace(input) { matchResult ->
                val key = matchResult.groupValues[1]
                vars?.get(key) ?: ""
            }
        }


        fun generateFromTemplateFromGitHub(
            project: Project,
            githubRepoUrl: String, // e.g. "https://github.com/user/repo"
            branch: String = "master",
            brickSubPath: String,  // e.g. "bricks/my_template"
            outputPath: String,
            vars: Map<String, String>? = null
        ) {
            try {
                val tempDir = Files.createTempDirectory("github_template").toFile()
                val zipUrl = "$githubRepoUrl/archive/refs/heads/$branch.zip"
                val zipFile = File(tempDir, "repo.zip")

                // Download ZIP
                URL(zipUrl).openStream().use { input ->
                    FileOutputStream(zipFile).use { output -> input.copyTo(output) }
                }

                // Extract ZIP
                val extractDir = File(tempDir, "extracted")
                unzip(zipFile, extractDir)

                // Locate brick path
                val repoName = githubRepoUrl.substringAfterLast("/")
                val brickPath = File(extractDir, "$repoName-$branch/$brickSubPath")
                if (!brickPath.exists()) throw Exception("Brick path not found in repo")

                // Use existing logic
                generateFromTemplate(
                    brickPath = brickPath.path,
                    outputPath = outputPath,
                    vars = vars
                )

                // Refresh VFS on UI thread
                ApplicationManager.getApplication().invokeLater {
                    val outputFile = File(outputPath)
                    val vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                        .refreshAndFindFileByIoFile(outputFile)
                    if (vFile != null) {
                        VfsUtil.markDirtyAndRefresh(true, true, true, vFile)
                    }
                }
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    showError(project, """"Failed to generate template: ${e.message}
                        Please check your network connection
                    """.trimMargin())
                }
                e.printStackTrace()
            }
        }

        private fun unzip(zipFile: File, targetDir: File) {
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile.mkdirs()
                        FileOutputStream(newFile).use { zipIn.copyTo(it) }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

    }


}