package eu.epitech.allinone.banana

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager
import eu.epitech.allinone.ui.EpitechToolWindowFactory

class CheckCurrentFileAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = e.project != null && file != null &&
            file.isInLocalFileSystem && BananaRunner.isCFile(file.name)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        project.service<BananaCheckService>().checkFile(file)
    }
}

class CheckProjectAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<BananaCheckService>()?.checkProject()
    }
}

class BuildWithBananaAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<BananaCheckService>()?.buildWithMake()
    }
}

class ClearBananaResultsAction : DumbAwareAction("Clear Results", null, AllIcons.Actions.GC) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project?.service<BananaCheckService>()?.lastResult != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<BananaCheckService>()?.clear()
    }
}

class ShowStyleGuideAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(BananaCheckService.TOOL_WINDOW_ID) ?: return
        toolWindow.show {
            toolWindow.contentManager.findContent(EpitechToolWindowFactory.GUIDE_TAB)?.let {
                toolWindow.contentManager.setSelectedContent(it, true)
            }
        }
    }
}
