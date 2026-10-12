package eu.epitech.allinone.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import eu.epitech.allinone.banana.BananaCheckService

class EpitechToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val factory = ContentFactory.getInstance()
        val manager = toolWindow.contentManager

        val report = BananaReportPanel(project)
        val reportContent = factory.createContent(report, REPORT_TAB, false)
        val output = BananaOutputPanel()
        val outputContent = factory.createContent(output, OUTPUT_TAB, false)
        val guideContent = factory.createContent(CodingStyleGuidePanel(), GUIDE_TAB, false)

        manager.addContent(reportContent)
        manager.addContent(outputContent)
        manager.addContent(guideContent)

        val service = project.service<BananaCheckService>()
        val listener = BananaCheckService.Listener { result ->
            report.show(result)
            output.show(result)
            if (result != null) manager.setSelectedContent(reportContent)
        }
        service.addListener(listener)
        Disposer.register(toolWindow.disposable, Disposable { service.removeListener(listener) })
        service.lastResult?.let { listener.resultChanged(it) }
    }

    companion object {
        const val REPORT_TAB = "Banana"
        const val OUTPUT_TAB = "Output"
        const val GUIDE_TAB = "Coding Style"
    }
}
