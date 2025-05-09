package core_plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ProjectRootManager
import utils.AppGenerator


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
        val projectRoot =
            ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return
        val rootPath = projectRoot.path  // Get the root path of the project

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading Core...", false) {
            override fun run(indicator: ProgressIndicator) {
                    AppGenerator.generateFromTemplateFromGitHub(
                        project = project,
                        githubRepoUrl = "https://github.com/OmarAly92/my_structure",
                        branch = "master",
                        brickSubPath = "lib/core",
                        outputPath = "$rootPath/lib/core",
                    )

            }
        })
    }
}