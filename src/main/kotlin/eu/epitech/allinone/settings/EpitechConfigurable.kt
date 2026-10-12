package eu.epitech.allinone.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import eu.epitech.allinone.banana.BananaLiveChecker

class EpitechConfigurable : BoundConfigurable("Epitech") {

    override fun createPanel(): DialogPanel {
        val o = EpitechSettings.options
        return panel {
            group("Header") {
                row("Author:") {
                    textField().align(AlignX.FILL)
                        .bindText({ o.author.orEmpty() }, { o.author = it.trim() })
                }
                row("Email:") {
                    textField().align(AlignX.FILL)
                        .bindText({ o.email.orEmpty() }, { o.email = it.trim() })
                }
                row {
                    checkBox("Add an \"Author: name <email>\" line in the file description")
                        .bindSelected(o::addAuthorLine)
                }
            }
            group("Banana (epiclang)") {
                row("epiclang executable:") {
                    textField().align(AlignX.FILL)
                        .bindText({ o.epiclangPath.orEmpty() }, { o.epiclangPath = it.trim().ifEmpty { "epiclang" } })
                }
                row {
                    checkBox("Check C files live while typing")
                        .bindSelected(o::liveCheck)
                }
                row("Live check delay (ms):") {
                    intTextField(100..10_000)
                        .bindIntText(o::liveCheckDelayMs)
                }
                row {
                    checkBox("Automatically add the project root and include/ as include directories")
                        .bindSelected(o::autoInclude)
                }
                row("Extra include directories (one per line, relative to the project root):") {}
                row {
                    textArea().rows(4).align(AlignX.FILL)
                        .bindText(
                            { o.includePaths.joinToString("\n") },
                            { text ->
                                o.includePaths.clear()
                                o.includePaths.addAll(text.lines().map(String::trim).filter(String::isNotEmpty))
                            },
                        )
                }
            }
        }
    }

    override fun apply() {
        super.apply()
        EpitechSettings.getInstance().state.intIncrementModificationCount()
        for (project in ProjectManager.getInstance().openProjects) {
            if (!project.isDisposed) project.service<BananaLiveChecker>().recheckOpenFiles()
        }
    }
}
