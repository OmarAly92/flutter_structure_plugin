///*
// * Copyright: Copyright (c) 2019 Arne Rantzen <arne@rantzen.net>
// * License: GPL-3
// * Last Edited: 07.12.19, 23:26
// */
//
//package screen_plugin.generator
//
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VfsUtil
//import com.intellij.openapi.vfs.VirtualFile
//import screen_plugin.ui.ScreenNotifier
//import java.io.IOException
//
///**
// * MvvmGenerator Factory to create structure
// */
//interface ScreenGenerator {
//    companion object {
//        /**
//         * Creates a [parent] folder and its [children] in a given [folder].
//         * [project] is needed for the notifications if there is an error or a warning situation.
//         * @return null if an error occurred or the a map of all virtual files created
//         */
//        fun createFolder(
//            project: Project,
//            folder: VirtualFile,
//            parent: String,
//            vararg children: String
//        ): Map<String, VirtualFile>? {
//            try {
//                for (child in folder.children) {
//                    if (child.name == parent) {
//                        ScreenNotifier.warning(project, "Directory [$parent] already exists")
//                        return null
//                    }
//                }
//                val mapOfFolder = mutableMapOf<String, VirtualFile>()
//                mapOfFolder[parent] = folder.createChildDirectory(folder, parent)
//                for (child in children) {
//                    mapOfFolder[child] =
//                        mapOfFolder[parent]?.createChildDirectory(mapOfFolder[parent], child)
//                            ?: throw IOException()
//                }
//                return mapOfFolder
//            } catch (e: IOException) {
//                ScreenNotifier.warning(project, "Couldn't create $parent directory")
//                e.printStackTrace()
//                return null
//            }
//        }
//
//        fun getFlutterPackageName(projectDir: VirtualFile): String? {
//            val pubspec = projectDir.findChild("pubspec.yaml") ?: return null
//            val text = VfsUtil.loadText(pubspec)
//            val nameLine = text.lineSequence().firstOrNull { it.trim().startsWith("name:") }
//            return nameLine?.split(":")?.getOrNull(1)?.trim()
//        }
//    }
//}