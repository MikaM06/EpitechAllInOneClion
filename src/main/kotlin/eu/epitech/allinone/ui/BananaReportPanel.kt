package eu.epitech.allinone.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import eu.epitech.allinone.banana.BananaCheckService
import eu.epitech.allinone.banana.BananaHighlighter
import eu.epitech.allinone.banana.BananaIssue
import eu.epitech.allinone.banana.BananaResult
import eu.epitech.allinone.banana.ClearBananaResultsAction
import com.intellij.icons.AllIcons
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BananaReportPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val root = DefaultMutableTreeNode()
    private val model = DefaultTreeModel(root)
    private val tree = Tree(model)
    private val summary = JBLabel("Run \"Check Coding Style\" to see Banana results.").apply {
        border = JBUI.Borders.empty(4, 8)
    }

    private data class FileNode(val path: String, val issues: List<BananaIssue>)

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = Renderer(project.service<BananaCheckService>().projectRoot())
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean = navigateToSelection()
        }.installOn(tree)
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && navigateToSelection()) e.consume()
            }
        })

        val actions = DefaultActionGroup().apply {
            val am = ActionManager.getInstance()
            add(am.getAction("epitech.checkFile"))
            add(am.getAction("epitech.checkProject"))
            add(am.getAction("epitech.buildWithBanana"))
            addSeparator()
            add(ClearBananaResultsAction())
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("EpitechBanana", actions, true)
        toolbar.targetComponent = this
        setToolbar(toolbar.component)

        val content = JPanel(BorderLayout())
        content.add(summary, BorderLayout.NORTH)
        content.add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
        setContent(content)
    }

    fun show(result: BananaResult?) {
        root.removeAllChildren()
        if (result == null) {
            summary.text = "Run \"Check Coding Style\" to see Banana results."
            summary.foreground = JBColor.foreground()
        } else {
            summary.text = result.summary()
            summary.foreground = when {
                result.error != null -> JBColor.RED
                result.issues.isEmpty() -> JBColor.namedColor("Label.successForeground", JBColor(0x368746, 0x5FB865))
                else -> JBColor.foreground()
            }
            result.issues.groupBy { it.filePath }.toSortedMap().forEach { (path, issues) ->
                val sorted = issues.sortedWith(compareBy({ it.line }, { it.column }))
                val fileNode = DefaultMutableTreeNode(FileNode(path, sorted))
                sorted.forEach { fileNode.add(DefaultMutableTreeNode(it)) }
                root.add(fileNode)
            }
        }
        model.reload()
        TreeUtil.expandAll(tree)
    }

    private fun navigateToSelection(): Boolean {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return false
        val issue = node.userObject as? BananaIssue ?: return false
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(issue.filePath) ?: return false
        OpenFileDescriptor(project, file, issue.line - 1, (issue.column - 1).coerceAtLeast(0)).navigate(true)
        return true
    }

    private class Renderer(private val projectRoot: Path?) : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
            leaf: Boolean, row: Int, hasFocus: Boolean,
        ) {
            when (val obj = (value as? DefaultMutableTreeNode)?.userObject) {
                is FileNode -> {
                    icon = AllIcons.FileTypes.Any_type
                    val path = Path.of(obj.path)
                    append(path.fileName?.toString() ?: obj.path, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    val shown = projectRoot?.takeIf { path.startsWith(it) }?.relativize(path)?.parent?.toString()
                        ?: path.parent?.toString()
                    if (!shown.isNullOrEmpty()) append("  $shown", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append("  ${obj.issues.size} issue(s)", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
                }
                is BananaIssue -> {
                    icon = BananaHighlighter.iconFor(obj.severity)
                    obj.rule?.let { append("$it  ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES) }
                    append("[${obj.severity.label}] ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(obj.message)
                    append("  ${obj.line}:${obj.column}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }
}
