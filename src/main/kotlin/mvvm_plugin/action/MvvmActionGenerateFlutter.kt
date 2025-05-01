package mvvm_plugin.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import mvvm_plugin.ui.MvvmFeatureDialog
import utils.AppGenerator
import utils.ScreenGenMethods

/**
 * Flutter action in the context menu
 *
 * This class will call the dialog and generate the Flutter Clean-Architecture structure
 */
class MvvmActionGenerateFlutter : AnAction() {


    /**
     * Is called by the context action menu entry with an [actionEvent]
     */
    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dialog = MvvmFeatureDialog(actionEvent.project)
        if (dialog.showAndGet()) {
            generate(actionEvent.dataContext, dialog.getName(), dialog.splitSource())
        }
    }

    /**
     * Generates the Flutter Clean-Architecture structure in a [dataContext].
     * If a [root] String is provided, it will create the structure in a new folder.
     */
    private fun generate(dataContext: DataContext, fileRoot: String?, splitSource: Boolean?) {
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
                    project, folder, root
                ) ?: return@runWriteCommandAction
                folder = result[root]
            }

            /**
             * Generates Data Layer
             */
            if (splitSource != null && splitSource) {
                val mapOrFalse = AppGenerator.createFolder(
                    project, folder,
                    "data",
                    "repository", "model"
                ) ?: return@runWriteCommandAction
                mapOrFalse["data"]?.let { data: VirtualFile ->
                    AppGenerator.createFolder(
                        project, data,
                        "data_source",
                        "remote", "local"
                    )


                    /**
                     * Generates Remote Data Source Dart file
                     */
                    val remoteDataSourceFolder =
                        mapOrFalse["data"]?.findChild("data_source")?.findChild("remote")
                    if (remoteDataSourceFolder != null && root != null) {
                        val content = getDataSourceFileContent(root, false)
                        AppGenerator.createDartFile(
                            remoteDataSourceFolder,
                            root + "_remote_data_source",
                            content
                        )
                    }

                    /**
                     * Generates Local Data Source Dart file
                     */
                    val localDataSourceFolder =
                        mapOrFalse["data"]?.findChild("data_source")?.findChild("local")
                    if (localDataSourceFolder != null && root != null) {
                        val content = getDataSourceFileContent(root, true)
                        AppGenerator.createDartFile(
                            localDataSourceFolder,
                            root + "_local_data_source",
                            content
                        )
                    }

                    /**
                     * Generates Repository Imp Dart file
                     */
                    val repositoryFolder = mapOrFalse["data"]?.findChild("repository")
                    if (repositoryFolder != null && root != null) {
                        val content = getRepositoryFileContent(root, flutterPackageName)
                        AppGenerator.createDartFile(repositoryFolder, root + "_repository", content)
                    }
                }
            } else {
                val dataResult = AppGenerator.createFolder(
                    project, folder,
                    "data",
                    "repository", "data_source", "model"
                )

                /**
                 * Generates Remote Data Source Dart file
                 */
                val dataSourceFolder = dataResult?.get("data")?.findChild("data_source")
                if (dataSourceFolder != null && root != null) {
                    val content = getDataSourceFileContent(root, false)
                    AppGenerator.createDartFile(
                        dataSourceFolder,
                        root + "_remote_data_source",
                        content
                    )
                }

                /**
                 * Generates Repository Imp Dart file
                 */
                val repositoryFolder = dataResult?.get("data")?.findChild("repository")
                if (repositoryFolder != null && root != null) {
                    val content = getRepositoryFileContent(root, flutterPackageName)
                    AppGenerator.createDartFile(repositoryFolder, root + "_repository", content)
                }
            }

            /**
             * Generates Presentation Layer
             */
            val presentationResult =
                AppGenerator.createFolder(project, folder, "presentation", "${root}_screen")
            val presentationFolder = presentationResult?.get("${root}_screen")
            if (presentationFolder != null) {
                val uiResult = AppGenerator.createFolder(project, presentationFolder, "ui")
                val uiFolder = uiResult?.get("ui")
                if (uiFolder != null) {
                    root?.let { ScreenGenMethods.getScreenFileContent(it, flutterPackageName,false) }
                        ?.let {
                            AppGenerator.createDartFile(
                                uiFolder,
                                "${root}_screen",
                                it
                            )
                        }
                    val widgetsResult = AppGenerator.createFolder(project, uiFolder, "widgets")
                    val widgetsFolder = widgetsResult?.get("widgets")
                    if (widgetsFolder != null) {
                        root?.let { ScreenGenMethods.getBodyWidgetFileContent(it) }?.let {
                            AppGenerator.createDartFile(
                                widgetsFolder,
                                "${root}_body",
                                it
                            )
                        }
                    }

                }
                val logicResult = AppGenerator.createFolder(project, presentationFolder, "logic")
                val logicFolder = logicResult?.get("logic")
                if (logicFolder != null) {
                    root?.let { ScreenGenMethods.getCubitFileContent(it) }?.let {
                        AppGenerator.createDartFile(
                            logicFolder,
                            "${root}_cubit",
                            it
                        )
                    }
                    root?.let { ScreenGenMethods.getStateFileContent(it) }?.let {
                        AppGenerator.createDartFile(
                            logicFolder,
                            "${root}_state",
                            it
                        )
                    }
                }
            }
        }
    }

    private fun getRepositoryFileContent(
        root: String,
        flutterPackageName: String,
    ): String {
        val className = AppGenerator.snakeToCamelCase(root) + "Repository"
        val content = """
          import 'package:$flutterPackageName/$root/data/data_source/${root}_remote_data_source.dart';  
            
          abstract class $className {}
           
          class ${className + "Imp"} implements $className  {
            ${className + "Imp"}(this._remoteDataSource);
           
            final ${AppGenerator.snakeToCamelCase(root)}RemoteDataSource _remoteDataSource;
          }
          """.trimIndent()
        return content
    }

    private fun getDataSourceFileContent(
        root: String,
        isLocal: Boolean = false,
    ): String {
        val className =
            AppGenerator.snakeToCamelCase(root) + "${if (isLocal) "Local" else "Remote"}DataSource"
        val content = """
           abstract class $className {}

           class ${className + "Imp"} implements $className  {}
          """.trimIndent()
        return content
    }
}