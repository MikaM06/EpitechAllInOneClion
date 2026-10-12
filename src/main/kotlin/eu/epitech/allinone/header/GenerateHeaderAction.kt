package eu.epitech.allinone.header

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import eu.epitech.allinone.settings.EpitechSettings
import java.time.Year
import javax.swing.JComponent

class GenerateHeaderAction : DumbAwareAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = e.project != null &&
            e.getData(CommonDataKeys.EDITOR) != null &&
            file != null && EpitechHeader.styleFor(file.name) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val style = EpitechHeader.styleFor(file.name) ?: return

        val dialog = HeaderDialog(project, project.name, EpitechHeader.defaultDescription(file.name))
        if (!dialog.showAndGet()) return

        val header = EpitechHeader.build(
            style, Year.now().value, dialog.projectName, dialog.description, authorLines(),
        )
        val document = editor.document
        WriteCommandAction.runWriteCommandAction(project, "Generate Epitech Header", null, {
            document.setText(EpitechHeader.apply(document.text, style, header))
        })
    }

    private fun authorLines(): List<String> {
        val o = EpitechSettings.options
        val author = o.author.orEmpty()
        if (!o.addAuthorLine || author.isBlank()) return emptyList()
        val email = o.email.orEmpty()
        return listOf(if (email.isBlank()) "Author: $author" else "Author: $author <$email>")
    }
}

private class HeaderDialog(
    project: Project,
    var projectName: String,
    var description: String,
) : DialogWrapper(project) {

    init {
        title = "Generate Epitech Header"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Project name:") {
            textField().bindText(::projectName).columns(30).align(AlignX.FILL).focused()
                .validationOnApply { if (EpitechHeader.isValidField(it.text)) null else error(FIELD_ERROR) }
        }
        row("File description:") {
            textField().bindText(::description).columns(30).align(AlignX.FILL)
                .validationOnApply { if (EpitechHeader.isValidField(it.text)) null else error(FIELD_ERROR) }
        }
    }

    private companion object {
        const val FIELD_ERROR = "At least 2 characters are required (C-G1)"
    }
}
