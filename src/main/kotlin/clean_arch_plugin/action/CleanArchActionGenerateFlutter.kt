package clean_arch_plugin.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import clean_arch_plugin.generator.CleanArchGenerator
import clean_arch_plugin.ui.CleanArchFeatureDialog
import java.io.IOException

/**
 * Flutter action in the context menu
 *
 * This class will call the dialog and generate the Flutter Clean-Architecture structure
 */
class CleanArchActionGenerateFlutter : AnAction() {


    /**
     * Is called by the context action menu entry with an [actionEvent]
     */
    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dialog = CleanArchFeatureDialog(actionEvent.project)
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
                val result = CleanArchGenerator.createFolder(
                    project, folder, root
                ) ?: return@runWriteCommandAction
                folder = result[root]
            }

            /**
             * Generates Data Layer
             */
            if (splitSource != null && splitSource) {
                val mapOrFalse = CleanArchGenerator.createFolder(
                    project, folder,
                    "data",
                    "repository", "model"
                ) ?: return@runWriteCommandAction
                mapOrFalse["data"]?.let { data: VirtualFile ->
                    CleanArchGenerator.createFolder(
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
                        val content = getRepositoryFileContent(root, false, project, selected)
                        createDartFile(repositoryFolder, root + "_repository", content)
                    }
                }
            } else {
                val dataResult = CleanArchGenerator.createFolder(
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
                    val content = getRepositoryFileContent(root, false, project, selected)
                    createDartFile(repositoryFolder, root + "_repository", content)
                }
            }


            /**
             * Generates Domain Layer
             */
            val domainResult = CleanArchGenerator.createFolder(
                project, folder,
                "domain",
                "repository", "use_case", "entity"
            )


            /**
             * Generates Abstract Repository Dart file
             */
            val repositoryFolder = domainResult?.get("domain")?.findChild("repository")
            if (repositoryFolder != null && root != null) {
                val content = getRepositoryFileContent(root, true, project, selected)
                createDartFile(repositoryFolder, root + "_repository", content)
            }

            /**
             * Generates Presentation Layer
             */
            val presentationResult =
                CleanArchGenerator.createFolder(project, folder, "presentation", "example_screen")
            val presentationFolder = presentationResult?.get("example_screen")
            if (presentationFolder != null) {
                val uiResult = CleanArchGenerator.createFolder(project, presentationFolder, "ui")
                val uiFolder = uiResult?.get("ui")
                if (uiFolder != null) {
                    CleanArchGenerator.createFolder(project, uiFolder, "widget")
                }
                CleanArchGenerator.createFolder(project, presentationFolder, "logic")
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
        isAbstract: Boolean = false,
        project: Project,
        selected: VirtualFile
    ): String {
        val className = snakeToCamelCase(root) + "Repository"
        if (isAbstract) {
            val content = """
           abstract class $className {}
          """.trimIndent()
            return content
        } else {
            val content = """
           import 'package:${project.name}/${selected.name}/$root/domain/repository/${root}_repository.dart';

           class ${className + "Imp"} implements $className  {}
          """.trimIndent()
            return content
        }
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
        return snake.split('_')
            .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}