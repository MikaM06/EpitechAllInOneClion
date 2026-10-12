package eu.epitech.allinone.banana

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.components.service
import eu.epitech.allinone.settings.EpitechSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class BananaLiveChecker(private val project: Project, private val scope: CoroutineScope) : Disposable {

    private val jobs = ConcurrentHashMap<Document, Job>()
    @Volatile private var missingToolReported = false

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                if (EditorFactory.getInstance().getEditors(document, project).isNotEmpty()) {
                    schedule(document, EpitechSettings.options.liveCheckDelayMs.toLong())
                }
            }
        }, this)
    }

    fun schedule(document: Document, delayMs: Long) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!file.isInLocalFileSystem || !BananaRunner.isCFile(file.name)) return
        if (!EpitechSettings.options.liveCheck) return
        val root = projectRoot(file)
        jobs.remove(document)?.cancel()
        jobs[document] = scope.launch {
            delay(delayMs)
            val stamp = document.modificationStamp
            val text = document.immutableCharSequence.toString()
            val result = withContext(Dispatchers.IO) { BananaRunner.checkContent(root, file.toNioPath(), text) }
            withContext(Dispatchers.EDT) {
                if (project.isDisposed || document.modificationStamp != stamp) return@withContext
                if (result.error != null) {
                    reportMissingTool(result.error)
                    return@withContext
                }
                BananaHighlighter.show(project, document, result.issues)
            }
        }
    }

    fun recheckOpenFiles() {
        val fdm = FileDocumentManager.getInstance()
        for (file in FileEditorManager.getInstance(project).openFiles) {
            val document = fdm.getCachedDocument(file) ?: continue
            if (EpitechSettings.options.liveCheck) {
                schedule(document, 0)
            } else {
                jobs.remove(document)?.cancel()
                BananaHighlighter.clear(project, document)
            }
        }
    }

    fun forget(file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getCachedDocument(file) ?: return
        jobs.remove(document)?.cancel()
    }

    private fun projectRoot(file: VirtualFile): Path =
        project.guessProjectDir()?.takeIf { it.isInLocalFileSystem }?.toNioPath()
            ?: file.parent.toNioPath()

    private fun reportMissingTool(message: String) {
        if (missingToolReported) return
        missingToolReported = true
        NotificationGroupManager.getInstance().getNotificationGroup("Epitech")
            .createNotification("Banana live check disabled for this session", message, NotificationType.WARNING)
            .addAction(NotificationAction.createSimpleExpiring("Open settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Epitech")
            })
            .notify(project)
    }

    override fun dispose() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}

class BananaFileEditorListener(private val project: Project) : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getCachedDocument(file) ?: return
        project.service<BananaLiveChecker>().schedule(document, 0)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        project.service<BananaLiveChecker>().forget(file)
    }
}

class BananaStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.EDT) { project.service<BananaLiveChecker>().recheckOpenFiles() }
    }
}
