/*
 * CUCUMBER +
 * Copyright (C) 2023  Maxime HAMM - NIMBLY CONSULTING - Maxime.HAMM@nimbly-consulting.com
 *
 * This document is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package io.nimbly.tzatziki.testdiscovery

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.ui.DebuggerColors
import io.nimbly.tzatziki.editor.TEST_IGNORED
import io.nimbly.tzatziki.editor.TEST_KO
import io.nimbly.tzatziki.editor.TEST_OK
import io.nimbly.tzatziki.pdf.escape
import io.nimbly.tzatziki.psi.fullRange
import io.nimbly.tzatziki.util.*
import org.jetbrains.plugins.cucumber.psi.*

object TzTestRegistry {

    private var activeResults = TzTestResult()
    private val filesWithResults = HashSet<PsiFile>()
    private val highlighters: MutableList<TzHighlight> = mutableListOf()

    fun refresh(results: TzTestResult) {

        // Clone previous results
        val temp = activeResults.clone()

        // Get involved scenario
        val involvedScenarios = HashSet<GherkinStepsHolder>()
        for (item in results.tests.values) {
            val scenario = item.scenario
                ?: continue
            involvedScenarios.add(scenario)
        }

        // Retain all related to not-involved scenarios
        temp.tests.entries.removeIf { involvedScenarios.contains(it.value.scenario) }

        // Add new results
        temp.putAll(results)

        // Add highlights
        results.tests.forEach { (element, _) ->
            highlighters += highlight(element, temp[element])
        }

        this.activeResults = temp
        rebuildFilesWithResults()
    }

    private fun highlight(element: GherkinPsiElement, tests: Set<SMTestProxy>): List<TzHighlight> {

        val highlights = mutableListOf<TzHighlight>()
        if (tests.isEmpty())
            return highlights

        val document = element.getDocument()
            ?: return highlights

        val editors = EditorFactory.getInstance().getEditors(document, element.project)
        if (editors.isEmpty())
            return highlights

        val textKey: TextAttributesKey
        if (tests.size == 1) {

            // Simple step or a cell
            val test = tests.first()
            textKey = test.textAttribut
        }
        else {

            // Step having examples
            var hasIgnored = false
            var hasKo = false
            for (test in tests) {
                if (test.isIgnored)
                    hasIgnored = true
                else if (test.isDefect)
                    hasKo = true
            }
            textKey = when {
                !hasIgnored && !hasKo -> TEST_OK
                hasKo -> TEST_KO
                else -> TEST_IGNORED
            }
        }

        // Add annotation
        editors.forEach { editor ->
            val range = element.bestRange()
            val markupModel = editor.markupModel
            highlights += TzHighlight(element.containingFile, markupModel, markupModel.addRangeHighlighter(
                textKey,
                range.startOffset,
                range.endOffset,
                DebuggerColors.EXECUTION_LINE_HIGHLIGHTERLAYER,
                HighlighterTargetArea.EXACT_RANGE
            ))
        }

        return highlights
    }

    fun clearHighlighters(file: PsiFile? = null) {
        highlighters.removeAll { h ->
            if (file == null || h.file == file) {
                h.model.removeHighlighter(h.highlight)
                true
            } else false
        }
    }

    fun cleanTestsResults(file: PsiFile, editor: Editor) {

        if (activeResults.tests.isEmpty())
            return

        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val scenario = PsiTreeUtil.getContextOfType(element, GherkinStepsHolder::class.java) ?: return

        // Retain all related to not-involved scenarios
        activeResults.tests.entries.removeIf { scenario == it.value.scenario }
        rebuildFilesWithResults()

    }

    fun cleanAllTestsResults(file: PsiFile) {

        clearHighlighters()

        if (activeResults.tests.isEmpty())
            return

        // Retain all related to not-involved scenarios
        activeResults.tests.entries.removeIf { file == it.value.scenario?.containingFile }
        rebuildFilesWithResults()

    }

    val results get() = activeResults

    fun hasResults(file: PsiFile): Boolean {
        return filesWithResults.contains(file)
    }

    private fun rebuildFilesWithResults() {
        filesWithResults.clear()
        for (item in activeResults.tests.values) {
            val file = item.scenario?.containingFile
                ?: continue
            filesWithResults.add(file)
        }
    }
}

data class TzHighlight(
    val file: PsiFile,
    val model: MarkupModel,
    val highlight: RangeHighlighter
)

class TzTestResult {

    internal var tests = mutableMapOf<GherkinPsiElement, TzTestItem>()

    fun putAll(results: TzTestResult) {
        results.tests.forEach { (elt, item) ->
            item.tests.forEach { t ->
                this[elt] = t
            }
        }
    }

    operator fun set(element: GherkinPsiElement, value: SMTestProxy) {
        var l = tests[element]
        if (l == null) {
            l = TzTestItem(element)
            tests[element] = l
        }
        l.add(value)
    }

    operator fun get(element: GherkinPsiElement): Set<SMTestProxy> {
        return tests[element]?.tests ?: emptySet()
    }

    fun clone(): TzTestResult {
        val r = TzTestResult()
        for ((key, value) in tests) {
            r.tests[key] = TzTestItem(key, value.scenario, value.tests.toMutableSet())
        }
        return r
    }

}

private val TOOLTIP_PREFIX_REGEX = "^(\\w*\\.)*\\w*: ".toRegex()

fun SMTestProxy.tooltip(): String {
    var t = this.stacktrace
    if (t.isNullOrBlank())
        return "Cucumber test failure"

    t = t.substringBefore("\n")
    t = t.replaceFirst(TOOLTIP_PREFIX_REGEX, "")
    t = t.escape()

    return "<html>$t</html>"
}

fun PsiElement.bestRange(): TextRange {

    if (this is GherkinTableRow)
        return textRange

    if (this is GherkinTableCell)
        return fullRange

    // Start after keyword
    var i = text.indexOf(" ")
    if (i<0)
        return textRange

    val t = text.substring(i)
    val j = t.indexOfFirst { it != ' ' }
    if (j>0)
        i += j

    // End at end ok line
    var length = text.indexOf("\n")
    if (length < 0) length = text.length

    return TextRange(textRange.startOffset + i, textRange.startOffset + length)
}


internal class TzTestItem(
    element: GherkinPsiElement,
    val scenario: GherkinStepsHolder? = element.parentScenario,
    val tests: MutableSet<SMTestProxy> = mutableSetOf()) {

    fun add(testProxy: SMTestProxy) {
        tests.add(testProxy)
    }
}

private val GherkinPsiElement.parentScenario
    get() = PsiTreeUtil.getNonStrictParentOfType(this, GherkinStepsHolder::class.java)
