package screen_plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.testFramework.utils.vfs.getFile
import screen_plugin.ui.ScreenDialog
import utils.AppGenerator
import utils.ScreenGenMethods

/**
 * Flutter action in the context menu
 *
 * This class will call the dialog and generate the Flutter Clean-Architecture structure
 */
class ScreenActionGenerateFlutter : AnAction() {
    /**
     * Is called by the context action menu entry with an [actionEvent]
     */
    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dialog = ScreenDialog(actionEvent.project)
        if (dialog.showAndGet()) {
            generate(actionEvent.dataContext, dialog.getName(), dialog.splitSource())
        }
    }

    /**
     * Generates the Flutter Clean-Architecture structure in a [dataContext].
     * If a [root] String is provided, it will create the structure in a new folder.
     */
    private fun generate(dataContext: DataContext, fileRoot: String?, withCubit: Boolean?) {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return
        val selected = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return
        val root = fileRoot?.let { AppGenerator.convertToSnakeCase(it) }
        var folder = if (selected.isDirectory) selected else selected.parent
        val projectRoot =
            ProjectRootManager.getInstance(project).contentRoots.firstOrNull() // Or use selected.findChild("..") if needed
        val flutterPackageName =
            projectRoot?.let { AppGenerator.getFlutterPackageName(it) } ?: "your_default_package"
        WriteCommandAction.runWriteCommandAction(project) {
            if (!root.isNullOrBlank()) {
                val result = AppGenerator.createFolder(
                    project, folder, "${root}_screen", "ui", "logic"
                ) ?: return@runWriteCommandAction
                folder = result[root]

                /**
                 * Generates Presentation Layer
                 */
                if (withCubit == false) {
                    val logicFolder = result["logic"]
                    if (logicFolder != null) {
                        AppGenerator.createDartFile(
                            logicFolder,
                            "${root}_cubit",
                            ScreenGenMethods.getCubitFileContent(root)
                        )
                        AppGenerator.createDartFile(
                            logicFolder,
                            "${root}_state",
                            ScreenGenMethods.getStateFileContent(root)
                        )
                    }
                }
                val uiFolder = result["ui"]
                if (uiFolder != null) {
                    AppGenerator.createDartFile(
                        uiFolder,
                        "${root}_screen",
                        ScreenGenMethods.getScreenFileContent(root, flutterPackageName)
                    )
                    val widgetsResult = AppGenerator.createFolder(project, uiFolder, "widgets")
                    val widgetsFolder = widgetsResult?.get("widgets")
                    if (widgetsFolder != null) {
                        AppGenerator.createDartFile(
                            widgetsFolder,
                            "${root}_body",
                            ScreenGenMethods.getBodyWidgetFileContent(root)
                        )
                    }
                }
            }
        }
    }
}