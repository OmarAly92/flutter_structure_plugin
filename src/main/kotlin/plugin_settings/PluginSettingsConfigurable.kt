package plugin_settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class PluginSettingsConfigurable : Configurable {


//    private val state: PluginSettingsState by lazy { PluginSettingsService.getInstance(project).state }
    private val apiKeyField: JPasswordField = JPasswordField()
    private val tokenField: JPasswordField = JPasswordField()
    private val fromListIdField: JTextField = JTextField()
    private val toListIdField: JTextField = JTextField()
    private val panel: JPanel = panel {
        row("API key") { apiKeyField }
        row("Token") { tokenField }
        row("From List Id") { fromListIdField }
        row("To List Id") { toListIdField }
    }


    override fun createComponent(): JComponent {
        return panel
    }

    override fun isModified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun apply() {
        TODO("Not yet implemented")
    }

    override fun getDisplayName(): String {
        TODO("Not yet implemented")
    }
}