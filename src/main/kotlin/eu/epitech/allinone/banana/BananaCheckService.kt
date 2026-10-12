package eu.epitech.allinone.banana

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
class BananaCheckService(private val project: Project) {

    fun interface Listener {
        fun resultChanged(result: BananaResult?)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()
    var lastResult: BananaResult? = null
        private set

    fun addListener(listener: Listener) = listeners.add(listener)
    fun removeListener(listener: Listener) = listeners.remove(listener)

    fun projectRoot(): Path? = project.guessProjectDir()?.takeIf { it.isInLocalFileSystem }?.toNioPath()

    fun checkFile(file: VirtualFile) {
        val root = projectRoot() ?: file.parent.toNioPath()
        run("Banana: checking ${file.name}") { BananaRunner.checkFiles(root, listOf(file.toNioPath())) }
    }

    fun checkProject() {
        val root = projectRoot() ?: return notifyError("The project has no local root directory")
        run("Banana: checking project") { BananaRunner.checkFiles(root, BananaRunner.collectSources(root)) }
    }

    fun buildWithMake() {
        val root = projectRoot() ?: return notifyError("The project has no local root directory")
        if (!root.resolve("Makefile").exists() && !root.resolve("makefile").exists()) {
            return notifyError("No Makefile at the project root. Use \"Check Coding Style (Project)\" instead.")
        }
        run("Banana: make -B CC=epiclang") { BananaRunner.buildWithMake(root) }
    }

    fun clear() {
        lastResult = null
        val fdm = FileDocumentManager.getInstance()
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            fdm.getCachedDocument(file)?.let { BananaHighlighter.clear(project, it) }
        }
        listeners.forEach { it.resultChanged(null) }
    }

    private fun run(title: String, check: () -> BananaResult) {
        FileDocumentManager.getInstance().saveAllDocuments()
        object : Task.Backgroundable(project, title, true) {
            private var result: BananaResult? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                result = check()
            }

            override fun onSuccess() {
                val res = result ?: return
                if (project.isDisposed) return
                publish(res)
            }
        }.queue()
    }

    private fun publish(result: BananaResult) {
        lastResult = result
        if (result.error != null) notifyError(result.error)
        highlightOpenFiles(result)
        listeners.forEach { it.resultChanged(result) }
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.show()
    }

    private fun highlightOpenFiles(result: BananaResult) {
        val byFile = result.issues.groupBy { it.filePath }
        val fdm = FileDocumentManager.getInstance()
        for (file in FileEditorManager.getInstance(project).openFiles) {
            if (!file.isInLocalFileSystem) continue
            val issues = byFile[file.toNioPath().toString()] ?: continue
            fdm.getCachedDocument(file)?.let { BananaHighlighter.show(project, it, issues) }
        }
    }

    private fun notifyError(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Epitech")
            .createNotification("Banana", message, NotificationType.ERROR)
            .addAction(NotificationAction.createSimpleExpiring("Open settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Epitech")
            })
            .notify(project)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Epitech"
    }
}
