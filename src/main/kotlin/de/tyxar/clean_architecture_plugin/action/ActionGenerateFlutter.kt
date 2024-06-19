/*
 * Copyright: Copyright (c) 2019 Arne Rantzen <arne@rantzen.net>
 * License: GPL-3
 * Last Edited: 08.12.19, 00:03
 */

package de.tyxar.clean_architecture_plugin.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import de.tyxar.clean_architecture_plugin.generator.Generator
import de.tyxar.clean_architecture_plugin.ui.FeatureDialog
import java.io.IOException

/**
 * Flutter action in the context menu
 *
 * This class will call the dialog and generate the Flutter Clean-Architecture structure
 */
class ActionGenerateFlutter : AnAction() {


    /**
     * Is called by the context action menu entry with an [actionEvent]
     */
    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dialog = FeatureDialog(actionEvent.project)
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
                val result = Generator.createFolder(
                    project, folder, root
                ) ?: return@runWriteCommandAction
                folder = result[root]
            }
            if (splitSource != null && splitSource) {
                val mapOrFalse = Generator.createFolder(
                    project, folder,
                    "data",
                    "repository", "model"
                ) ?: return@runWriteCommandAction
                mapOrFalse["data"]?.let { data: VirtualFile ->
                    Generator.createFolder(
                        project, data,
                        "data_source",
                        "remote", "local"
                    )
                }
            } else {
                val dataResult = Generator.createFolder(
                    project, folder,
                    "data",
                    "repository", "data_source", "model"
                )

                val repositoryFolder = dataResult?.get("data")?.findChild("repository")
                if (repositoryFolder != null && root != null) {
                    val content = getRepositoryFileContent(root, false, project)
                    createDartFile(repositoryFolder, root + "_repository", content)
                }
            }

            /// domain layer
            val domainResult = Generator.createFolder(
                project, folder,
                "domain",
                "repository", "use_case", "entity"
            )
            // Create a Dart file with the name of the root inside the repository folder in the domain
            val repositoryFolder = domainResult?.get("domain")?.findChild("repository")
            if (repositoryFolder != null && root != null) {
                val content = getRepositoryFileContent(root, true, project)
                createDartFile(repositoryFolder, root + "_repository", content)
            }

            /// ui folder
            val uiResult = Generator.createFolder(project, folder, "presentation", "logic", "ui")
            val uiFolder = uiResult?.get("ui")
            if (uiFolder != null) {
                Generator.createFolder(project, uiFolder, "widgets")
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
        project: Project
    ): String {
        val className = snakeToCamelCase(root) + "Repository"
        if (isAbstract) {
            val content = """
           abstract class $className {}
          """.trimIndent()
            return content
        } else {
            val content = """
           import 'package:${project.name}/features/aut/domain/repository/aut_repository.dart';

           class ${className + "Imp"} implements $className  {}
          """.trimIndent()
            return content
        }
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
        return snake.split('_').joinToString("") { it.capitalize() }
    }
}