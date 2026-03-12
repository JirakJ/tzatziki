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

package io.nimbly.tzatziki.editor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import icons.ActionIcons
import io.nimbly.tzatziki.psi.*
import io.nimbly.tzatziki.util.findTableAt
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinTable
import org.jetbrains.plugins.cucumber.psi.GherkinTableCell
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow
import org.jetbrains.plugins.cucumber.psi.impl.GherkinTableHeaderRowImpl

class TzCellCompletion: CompletionContributor() {

    fun complete(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {

        val cell = parameters.position.parent
        if (cell !is GherkinTableCell)
            return

        val file = cell.containingFile
        if (file !is GherkinFile)
            return
        val tables = file.findAllTables()

        if (cell.parent is GherkinTableHeaderRowImpl) {
            completeHeader(cell, tables, resultSet)
        }
        else {
            completeData(cell, tables, resultSet)
        }
    }

    /**
     * Complete Header
     */
    private fun completeHeader(cell: GherkinTableCell, tables: List<GherkinTable>, resultSet: CompletionResultSet) {

        // Find all values
        @Suppress("UNCHECKED_CAST")
        val values: Set<String> = tables
            .mapNotNull { it.headerRow }
            .flatMap { it.psiCells as List<GherkinTableCell> }
            .filter { it != cell }
            .map { it.text.trim() }
            .filterTo(mutableSetOf()) { it.isNotBlank() }

        // Create a new cell to chain completion
        var suffix = ""
        var cursorOffset = 0
        if (cell.row.table.dataRows.isEmpty() && cell.row.psiCells.indexOf(cell) == cell.row.psiCells.lastIndex) {
            suffix = " | "
            cursorOffset = 0
            if (cell.nextSibling == null  ) {
                suffix += " |"
                cursorOffset = -2
            }
        }

        // Add all values to completion
        values.forEach { value ->
            val lookup = LookupElementBuilder.create(value)
                .withPresentableText(value)
                .withIcon(ActionIcons.CUCUMBER_PLUS_16)
                .withInsertHandler { context, _ ->
                    if (cursorOffset != 0 && context.completionChar == '\t') {
                        EditorModificationUtil.insertStringAtCaret(context.editor, suffix, false, false)
                        context.editor.caretModel.moveToOffset(context.tailOffset + cursorOffset)
                    }
                    context.editor.findTableAt(context.startOffset)?.format(true)
                }
            resultSet.addElement(lookup)
        }
    }

    /**
     * Complete Data
     */
    private fun completeData(cell: GherkinTableCell, tables: List<GherkinTable>, resultSet: CompletionResultSet) {

        // Find column name
        @Suppress("UNCHECKED_CAST")
        val headerCells = cell.row.table.headerRow?.psiCells as? List<GherkinTableCell> ?: return
        val columnName = headerCells.getOrNull(cell.columnNumber)?.text?.trim()
            ?: return

        // Build frequency map directly (avoids intermediate list + groupingBy)
        val valueFreq = mutableMapOf<String, Int>()
        tables.forEach { table ->
            val header = table.headerRow
            if (header != null) {
                @Suppress("UNCHECKED_CAST")
                val hdrCells = header.psiCells as List<GherkinTableCell>
                val index = hdrCells.indexOfFirst { it.text.trim() == columnName }
                if (index >= 0) {
                    @Suppress("UNCHECKED_CAST")
                    val rows = table.dataRows as List<GherkinTableRow>
                    for (row in rows) {
                        val c = row.cell(index)
                        if (c != cell) {
                            val txt = c.text.trim()
                            if (txt.isNotBlank())
                                valueFreq[txt] = (valueFreq[txt] ?: 0) + 1
                        }
                    }
                }
            }
        }

        // Add all values to completion
        valueFreq.forEach { (value, count) ->
            val lookup = LookupElementBuilder.create(value)
                .withPresentableText(value)
                .withTypeText("(used $count times)")
                .withIcon(ActionIcons.CUCUMBER_PLUS_16)
                .withInsertHandler { context, _ ->
                    context.editor.findTableAt(context.startOffset)?.format(false)
                }
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookup, 100.0*count))
        }
    }


    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet)
                    = complete(parameters, context, resultSet)
            }
        )
    }
}