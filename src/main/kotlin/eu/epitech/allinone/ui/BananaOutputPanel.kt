package eu.epitech.allinone.ui

import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import eu.epitech.allinone.banana.BananaResult
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel

class BananaOutputPanel : JPanel(BorderLayout()) {

    private val text = JBTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().size)
        border = JBUI.Borders.empty(6)
    }

    init {
        add(ScrollPaneFactory.createScrollPane(text, true), BorderLayout.CENTER)
    }

    fun show(result: BananaResult?) {
        text.text = if (result == null) "" else "$ ${result.commandLine}\n\n${result.rawOutput}"
        text.caretPosition = 0
    }
}
