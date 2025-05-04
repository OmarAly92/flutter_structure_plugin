package core_plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ProjectRootManager
import utils.AppGenerator
import java.io.File


/**
 * Flutter action in the context menu
 *
 * This class will call the dialog and generate the Flutter Clean-Architecture structure
 */
class CoreActionGenerateFlutter : AnAction() {
    /**
     * Is called by the context action menu entry with an [actionEvent]
     */
    override fun actionPerformed(actionEvent: AnActionEvent) {
            generate(actionEvent.dataContext)
    }

    /**
     * Generates the Flutter Clean-Architecture structure in a [dataContext].
     * If a [root] String is provided, it will create the structure in a new folder.
     */
    private fun generate(dataContext: DataContext) {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val projectRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()
        val flutterPackageName = projectRoot?.let { AppGenerator.getFlutterPackageName(it) } ?: "your_default_package"

        val toolsDir = File(projectRoot?.path + "/lib/tools")
        toolsDir.mkdirs()

        // Copy replace_package_name_to_core.dart
        val replaceScriptStream = javaClass.classLoader.getResourceAsStream("replace_package_name_to_core.dart")
        val replaceScriptPath = toolsDir.path + "/replace_package_name_to_core.dart"
        val replaceScriptFile = File(replaceScriptPath)

        replaceScriptStream?.use { input ->
            replaceScriptFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return

        // Optionally: Copy remove_core.dart if it's also a bundled resource
        val removeScriptStream = javaClass.classLoader.getResourceAsStream("remove_core.dart")
        val removeScriptPath = toolsDir.path + "/remove_core.dart"
        val removeScriptFile = File(removeScriptPath)

        removeScriptStream?.use { input ->
            removeScriptFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            AppGenerator.runInTerminalSmart(
                project,
                """
            dart lib/tools/remove_core.dart
            mason make core
            dart lib/tools/replace_package_name_to_core.dart $flutterPackageName
            """.trimIndent()
            )
        }
    }
}