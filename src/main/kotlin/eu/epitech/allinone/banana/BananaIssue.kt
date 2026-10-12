package eu.epitech.allinone.banana

enum class BananaSeverity(val label: String) {
    FATAL("Fatal"),
    MAJOR("Major"),
    MINOR("Minor"),
    INFO("Info"),
    ERROR("Error"),
    WARNING("Warning");

    val isCodingStyle: Boolean get() = this == FATAL || this == MAJOR || this == MINOR || this == INFO

    companion object {
        fun fromBanana(label: String): BananaSeverity =
            entries.firstOrNull { it.isCodingStyle && it.label.equals(label, ignoreCase = true) } ?: MINOR
    }
}

data class BananaIssue(
    val filePath: String,
    val line: Int,
    val column: Int,
    val severity: BananaSeverity,
    val message: String,
    val rule: String?,
) {
    val displayText: String
        get() = buildString {
            if (rule != null) append("[$rule] ")
            append(severity.label).append(": ").append(message)
        }
}

data class BananaResult(
    val issues: List<BananaIssue>,
    val rawOutput: String,
    val commandLine: String,
    val error: String? = null,
) {
    fun count(severity: BananaSeverity) = issues.count { it.severity == severity }

    fun summary(): String {
        if (error != null) return error
        if (issues.isEmpty()) return "No coding style issue found"
        return BananaSeverity.entries
            .map { it to count(it) }
            .filter { it.second > 0 }
            .joinToString(", ") { "${it.second} ${it.first.label}" } +
            " in ${issues.map { it.filePath }.distinct().size} file(s)"
    }
}
