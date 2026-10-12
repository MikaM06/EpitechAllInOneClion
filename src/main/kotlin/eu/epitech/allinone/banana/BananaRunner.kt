package eu.epitech.allinone.banana

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import eu.epitech.allinone.settings.EpitechSettings
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.writeText

object BananaRunner {

    private const val TIMEOUT_MS = 60_000
    private const val MAKE_TIMEOUT_MS = 10 * 60_000
    private val SKIPPED_DIRS = setOf("build", "out", "node_modules", "venv", "tests_build")
    val C_EXTENSIONS = setOf("c", "h")

    fun isCFile(fileName: String) = fileName.substringAfterLast('.', "").lowercase() in C_EXTENSIONS

    fun checkFiles(projectRoot: Path, files: List<Path>): BananaResult {
        if (files.isEmpty()) return BananaResult(emptyList(), "", "", "No .c/.h file to check")
        val args = baseArgs(projectRoot, extraDirs = emptyList()) + files.map { it.toString() }
        return execute(args, projectRoot, TIMEOUT_MS) { BananaOutputParser.parse(it, projectRoot) }
    }

    /**
     * Checks unsaved editor content: the text is written to a temporary copy with the same
     * file name, so header-specific rules still apply, then results are mapped back.
     */
    fun checkContent(projectRoot: Path, realFile: Path, content: String): BananaResult {
        val tempDir = Files.createTempDirectory("epitech-banana")
        try {
            val copy = tempDir.resolve(realFile.name)
            copy.writeText(content)
            val extraDirs = listOfNotNull(realFile.parent)
            val args = baseArgs(projectRoot, extraDirs) + copy.toString()
            val result = execute(args, realFile.parent ?: projectRoot, TIMEOUT_MS) { output ->
                BananaOutputParser.parse(output, realFile.parent ?: projectRoot)
                    .filter { it.filePath == copy.toString() }
                    .map { it.copy(filePath = realFile.toString()) }
            }
            return result
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /** Runs `make -B CC=epiclang` so every translation unit is recompiled through Banana. */
    fun buildWithMake(projectRoot: Path): BananaResult {
        val epiclang = EpitechSettings.options.epiclangPath.orEmpty().ifBlank { "epiclang" }
        val args = listOf("make", "-B", "CC=$epiclang")
        return execute(args, projectRoot, MAKE_TIMEOUT_MS) { BananaOutputParser.parse(it, projectRoot) }
    }

    fun collectSources(projectRoot: Path): List<Path> {
        if (!projectRoot.isDirectory()) return emptyList()
        return Files.walk(projectRoot).use { stream ->
            stream.filter { path ->
                val rel = projectRoot.relativize(path)
                rel.none { it.name.startsWith(".") || it.name.startsWith("cmake-build-") || it.name in SKIPPED_DIRS } &&
                    Files.isRegularFile(path) && isCFile(path.name)
            }.sorted().toList()
        }
    }

    private fun baseArgs(projectRoot: Path, extraDirs: List<Path>): List<String> {
        val o = EpitechSettings.options
        val includeDirs = LinkedHashSet<Path>()
        includeDirs.addAll(extraDirs)
        if (o.autoInclude) {
            includeDirs.add(projectRoot)
            projectRoot.resolve("include").takeIf { it.isDirectory() }?.let { includeDirs.add(it) }
        }
        o.includePaths.forEach { includeDirs.add(projectRoot.resolve(it).normalize()) }
        return listOf(o.epiclangPath.orEmpty().ifBlank { "epiclang" }, "-Wno-everything", "-fsyntax-only") +
            includeDirs.map { "-I$it" }
    }

    private fun execute(
        args: List<String>,
        workDir: Path,
        timeoutMs: Int,
        parse: (String) -> List<BananaIssue>,
    ): BananaResult {
        val commandLine = GeneralCommandLine(args)
            .withWorkingDirectory(workDir)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withEnvironment("LC_ALL", "C")
            .withRedirectErrorStream(true)
        val shown = commandLine.commandLineString
        return try {
            val output = CapturingProcessHandler(commandLine).runProcess(timeoutMs)
            if (output.isTimeout) return BananaResult(emptyList(), output.stdout, shown, "Timed out: $shown")
            BananaResult(parse(output.stdout), output.stdout, shown)
        } catch (e: ExecutionException) {
            BananaResult(
                emptyList(), e.message.orEmpty(), shown,
                "Cannot run '${args.first()}': ${e.message}. Check Settings > Tools > Epitech.",
            )
        }
    }
}
