package eu.epitech.allinone.header

enum class CommentStyle(val open: String, val prefix: String, val close: String) {
    C_BLOCK("/*", "**", "*/"),
    HASH("##", "##", "##"),
}

object EpitechHeader {

    private val C_BLOCK_EXTENSIONS = setOf(
        "c", "h", "cpp", "hpp", "cc", "hh", "cxx", "hxx", "tpp", "ipp", "inl",
        "js", "jsx", "ts", "tsx", "mjs", "cjs", "java", "kt", "cs", "go", "rs",
    )
    private val HASH_EXTENSIONS = setOf("py", "sh", "bash", "zsh", "mk", "cmake", "rb", "pl")
    private val MAKEFILE_NAMES = setOf("makefile", "gnumakefile", "cmakelists.txt")

    private val C_BLOCK_HEADER = Regex("""\A/\*\n\*\* EPITECH PROJECT, \d{4}\n(?:\*\*.*\n)*?\*/(?:\n|\z)""")
    private val HASH_HEADER = Regex("""\A##\n## EPITECH PROJECT, \d{4}\n(?:##.*\n)*?##(?:\n|\z)""")

    fun styleFor(fileName: String): CommentStyle? {
        val lower = fileName.lowercase()
        if (lower in MAKEFILE_NAMES) return CommentStyle.HASH
        val ext = lower.substringAfterLast('.', "")
        return when (ext) {
            in C_BLOCK_EXTENSIONS -> CommentStyle.C_BLOCK
            in HASH_EXTENSIONS -> CommentStyle.HASH
            else -> null
        }
    }

    fun defaultDescription(fileName: String): String {
        if (fileName.lowercase() in MAKEFILE_NAMES) return fileName
        return fileName.substringBeforeLast('.').ifEmpty { fileName }
    }

    /** Banana's C-G1 check requires at least two characters after "** ". */
    fun isValidField(value: String): Boolean = Regex("""\S.+""").matches(value.trim())

    fun build(
        style: CommentStyle,
        year: Int,
        projectName: String,
        description: String,
        extraDescriptionLines: List<String> = emptyList(),
    ): String = buildString {
        val p = style.prefix
        appendLine(style.open)
        appendLine("$p EPITECH PROJECT, $year")
        appendLine("$p ${projectName.trim()}")
        appendLine("$p File description:")
        appendLine("$p ${description.trim()}")
        extraDescriptionLines.forEach { appendLine("$p $it") }
        appendLine(style.close)
    }

    /**
     * Returns [text] with [header] placed at its top, replacing any existing Epitech header
     * and keeping a shebang line first for script files.
     */
    fun apply(text: String, style: CommentStyle, header: String): String {
        var shebang = ""
        var body = text
        if (style == CommentStyle.HASH && body.startsWith("#!")) {
            val end = body.indexOf('\n').let { if (it < 0) body.length else it + 1 }
            shebang = body.substring(0, end).let { if (it.endsWith("\n")) it else it + "\n" }
            body = body.substring(end)
        }
        val existing = (if (style == CommentStyle.C_BLOCK) C_BLOCK_HEADER else HASH_HEADER).find(body)
        if (existing != null) body = body.substring(existing.range.last + 1)
        body = body.trimStart('\n', '\r')
        return if (body.isEmpty()) shebang + header else shebang + header + "\n" + body
    }
}
