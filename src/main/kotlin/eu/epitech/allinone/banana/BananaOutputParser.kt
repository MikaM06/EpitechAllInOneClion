package eu.epitech.allinone.banana

import java.nio.file.Path

object BananaOutputParser {

    private val DIAGNOSTIC = Regex("""^(.+?):(\d+):(\d+): (fatal error|error|warning): (.*)$""")
    private val BANANA = Regex("""^\[Banana] \[(\w+)] (.*?)(?: \((C-[A-Z]\d+)\))?$""")
    private val MAKE_ENTER = Regex("""^g?make(?:\[\d+])?: Entering directory [`'](.+)'$""")
    private val MAKE_LEAVE = Regex("""^g?make(?:\[\d+])?: Leaving directory [`'](.+)'$""")

    /**
     * Parses clang/epiclang diagnostics. Relative paths are resolved against [baseDir], or
     * against the current directory announced by make ("Entering directory") when present.
     */
    fun parse(output: String, baseDir: Path): List<BananaIssue> {
        val dirs = ArrayDeque<Path>().apply { addLast(baseDir) }
        val issues = LinkedHashSet<BananaIssue>()
        for (rawLine in output.lineSequence()) {
            val line = rawLine.trimEnd('\r')
            MAKE_ENTER.matchEntire(line)?.let { dirs.addLast(Path.of(it.groupValues[1])); continue }
            MAKE_LEAVE.matchEntire(line)?.let { if (dirs.size > 1) dirs.removeLast(); continue }
            val match = DIAGNOSTIC.matchEntire(line) ?: continue
            val (path, lineNo, col, kind, text) = match.destructured
            issues += toIssue(resolve(dirs.last(), path), lineNo.toInt(), col.toInt(), kind, text)
        }
        return issues.toList()
    }

    private fun toIssue(path: String, line: Int, col: Int, kind: String, text: String): BananaIssue {
        val banana = BANANA.matchEntire(text)
        if (banana != null) {
            val (severity, message, rule) = banana.destructured
            return BananaIssue(path, line, col, BananaSeverity.fromBanana(severity), message, rule.ifEmpty { null })
        }
        val severity = if (kind == "warning") BananaSeverity.WARNING else BananaSeverity.ERROR
        return BananaIssue(path, line, col, severity, text, null)
    }

    private fun resolve(base: Path, path: String): String {
        if (path == "<stdin>") return path
        return base.resolve(path).normalize().toString()
    }
}
