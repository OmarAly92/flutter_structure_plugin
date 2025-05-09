package utils

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.application.PathManager
import java.io.File

class FirstRunCommandRunner : StartupActivity {

    override fun runActivity(project: Project) {
//        val pluginId = PluginId.getId("de.omar.clean_architecture_plugin")
//        val plugin = PluginManagerCore.getPlugin(pluginId)
//        val currentVersion = plugin?.version ?: return
//
//        val pluginConfigDir = File(PathManager.getConfigPath(), "flutter_structure_plugin")
//        if (!pluginConfigDir.exists()) pluginConfigDir.mkdirs()
//
//        val versionFile = File(pluginConfigDir, "last_version.txt")
//        val lastVersion = if (versionFile.exists()) versionFile.readText().trim() else null
//
//        if (lastVersion != currentVersion) {
//            runMasonCommand(project)
//            versionFile.writeText(currentVersion)
//        }
    }

    private fun runMasonCommand(project: Project) {
        try {
            val masonCommand = """
                dart pub global activate mason_cli
                mason remove core --global
                mason add core --git-url https://github.com/OmarAly92/my_structure.git --git-path bricks/core_brick --git-ref my_structure_mason --global
            """.trimIndent()

            AppGenerator.runInTerminalSmart(project, masonCommand)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}