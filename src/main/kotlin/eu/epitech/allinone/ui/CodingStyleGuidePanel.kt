package eu.epitech.allinone.ui

import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JEditorPane
import javax.swing.JPanel

class CodingStyleGuidePanel : JPanel(BorderLayout()) {

    init {
        val html = javaClass.getResource("/guide/coding-style.html")?.readText()
            ?: "<html><body>Coding style guide not found.</body></html>"
        val pane = JEditorPane().apply {
            editorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()
            isEditable = false
            border = JBUI.Borders.empty(8, 12)
            text = html
            caretPosition = 0
        }
        add(ScrollPaneFactory.createScrollPane(pane, true), BorderLayout.CENTER)
    }
}
