package mvvm_plugin.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import mvvm_plugin.generator.MvvmGenerator
import mvvm_plugin.ui.MvvmFeatureDialog
import java.io.IOException

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
        val root = fileRoot?.let { convertToSnakeCase(it) }
        var folder = if (selected.isDirectory) selected else selected.parent
        WriteCommandAction.runWriteCommandAction(project) {
            if (!root.isNullOrBlank()) {
                val result = MvvmGenerator.createFolder(
                    project, folder, root
                ) ?: return@runWriteCommandAction
                folder = result[root]
            }

            /**
             * Generates Data Layer
             */
            if (splitSource != null && splitSource) {
                val mapOrFalse = MvvmGenerator.createFolder(
                    project, folder,
                    "data",
                    "repository", "model"
                ) ?: return@runWriteCommandAction
                mapOrFalse["data"]?.let { data: VirtualFile ->
                    MvvmGenerator.createFolder(
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
                        createDartFile(
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
                        createDartFile(localDataSourceFolder, root + "_local_data_source", content)
                    }

                    /**
                     * Generates Repository Imp Dart file
                     */
                    val repositoryFolder = mapOrFalse["data"]?.findChild("repository")
                    if (repositoryFolder != null && root != null) {
                        val content = getRepositoryFileContent(root,  project, selected)
                        createDartFile(repositoryFolder, root + "_repository", content)
                    }
                }
            } else {
                val dataResult = MvvmGenerator.createFolder(
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
                    createDartFile(dataSourceFolder, root + "_remote_data_source", content)
                }

                /**
                 * Generates Repository Imp Dart file
                 */
                val repositoryFolder = dataResult?.get("data")?.findChild("repository")
                if (repositoryFolder != null && root != null) {
                    val content = getRepositoryFileContent(root,  project, selected)
                    createDartFile(repositoryFolder, root + "_repository", content)
                }
            }

            /**
             * Generates Presentation Layer
             */
            val presentationResult =
                MvvmGenerator.createFolder(project, folder, "presentation", "example_screen")
            val presentationFolder = presentationResult?.get("example_screen")
            if (presentationFolder != null) {
                val uiResult = MvvmGenerator.createFolder(project, presentationFolder, "ui")
                val uiFolder = uiResult?.get("ui")
                if (uiFolder != null) {
                    MvvmGenerator.createFolder(project, uiFolder, "widget")
                }
                MvvmGenerator.createFolder(project, presentationFolder, "logic")
            }
        }
    }

    private fun createDartFile(
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

    private fun getRepositoryFileContent(
        root: String,
        project: Project,
        selected: VirtualFile
    ): String {
        val className = snakeToCamelCase(root) + "Repository"
        val content = """
           import 'package:${project.name}/${selected.name}/$root/domain/repository/${root}_repository.dart';

           class ${className + "Imp"} implements $className  {}
          """.trimIndent()
        return content
    }

    private fun getDataSourceFileContent(
        root: String,
        isLocal: Boolean = false,
    ): String {
        val className = snakeToCamelCase(root) + "${if (isLocal) "Local" else "Remote"}DataSource"
        val content = """
           abstract class $className {}

           class ${className + "Imp"} implements $className  {}
          """.trimIndent()
        return content
    }

    private fun convertToSnakeCase(input: String): String {
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


    private fun snakeToCamelCase(snake: String): String {
        return snake.split('_').joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}