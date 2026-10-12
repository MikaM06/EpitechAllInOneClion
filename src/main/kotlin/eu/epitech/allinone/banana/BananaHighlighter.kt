package eu.epitech.allinone.banana

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import javax.swing.Icon

/**
 * Draws Banana results with plain markup highlighters: this works whatever C/C++ engine
 * CLion uses (classic or Nova), since it does not depend on a PSI language.
 */
object BananaHighlighter {

    private val HIGHLIGHTERS = Key.create<List<RangeHighlighter>>("epitech.banana.highlighters")

    fun show(project: Project, document: Document, issues: List<BananaIssue>) {
        clear(project, document)
        if (issues.isEmpty()) return
        val markup = DocumentMarkupModel.forDocument(document, project, true)
        val created = mutableListOf<RangeHighlighter>()
        val firstOfLine = HashSet<Int>()
        for (issue in issues.sortedBy { it.severity.ordinal }) {
            val range = rangeFor(document, issue) ?: continue
            val highlighter = markup.addRangeHighlighter(
                attributesFor(issue.severity), range.startOffset, range.endOffset,
                layerFor(issue.severity), HighlighterTargetArea.EXACT_RANGE,
            )
            highlighter.errorStripeTooltip = "Banana " + issue.displayText
            if (firstOfLine.add(issue.line)) {
                val lineIssues = issues.filter { it.line == issue.line }.sortedBy { it.severity.ordinal }
                highlighter.gutterIconRenderer = BananaGutterIcon(lineIssues)
            }
            created += highlighter
        }
        document.putUserData(HIGHLIGHTERS, created)
    }

    fun clear(project: Project, document: Document) {
        val old = document.getUserData(HIGHLIGHTERS) ?: return
        document.putUserData(HIGHLIGHTERS, null)
        val markup = DocumentMarkupModel.forDocument(document, project, false) ?: return
        old.filter { it.isValid }.forEach { markup.removeHighlighter(it) }
    }

    private fun rangeFor(document: Document, issue: BananaIssue): TextRange? {
        if (document.lineCount == 0) return null
        val line = (issue.line - 1).coerceIn(0, document.lineCount - 1)
        val lineStart = document.getLineStartOffset(line)
        val lineEnd = document.getLineEndOffset(line)
        if (lineStart == lineEnd) return TextRange(lineStart, lineStart)
        val text = document.charsSequence
        var start = (lineStart + issue.column - 1).coerceIn(lineStart, lineEnd)
        if (start == lineEnd) start = lineEnd - 1
        var end = start + 1
        if (StringUtil.isJavaIdentifierPart(text[start])) {
            while (end < lineEnd && StringUtil.isJavaIdentifierPart(text[end])) end++
        } else if (text[start] == ' ' || text[start] == '\t') {
            while (end < lineEnd && (text[end] == ' ' || text[end] == '\t')) end++
        }
        return TextRange(start, end)
    }

    private fun attributesFor(severity: BananaSeverity): TextAttributesKey = when (severity) {
        BananaSeverity.FATAL, BananaSeverity.MAJOR, BananaSeverity.ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
        BananaSeverity.MINOR, BananaSeverity.WARNING -> CodeInsightColors.WARNINGS_ATTRIBUTES
        BananaSeverity.INFO -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
    }

    private fun layerFor(severity: BananaSeverity): Int = when (severity) {
        BananaSeverity.FATAL, BananaSeverity.MAJOR, BananaSeverity.ERROR -> HighlighterLayer.ERROR
        else -> HighlighterLayer.WARNING
    }

    fun iconFor(severity: BananaSeverity): Icon = when (severity) {
        BananaSeverity.FATAL, BananaSeverity.MAJOR, BananaSeverity.ERROR -> AllIcons.General.Error
        BananaSeverity.MINOR, BananaSeverity.WARNING -> AllIcons.General.Warning
        BananaSeverity.INFO -> AllIcons.General.Information
    }
}

private class BananaGutterIcon(private val issues: List<BananaIssue>) : GutterIconRenderer() {
    override fun getIcon(): Icon = BananaHighlighter.iconFor(issues.first().severity)
    override fun getTooltipText(): String =
        issues.joinToString("<br>", prefix = "<html><b>Banana</b><br>", postfix = "</html>") {
            StringUtil.escapeXmlEntities(it.displayText)
        }
    override fun equals(other: Any?) = other is BananaGutterIcon && other.issues == issues
    override fun hashCode() = issues.hashCode()
}
