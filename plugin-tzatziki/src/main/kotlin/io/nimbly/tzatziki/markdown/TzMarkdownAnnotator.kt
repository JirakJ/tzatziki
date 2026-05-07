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

package io.nimbly.tzatziki.markdown

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.nimbly.tzatziki.TOGGLE_CUCUMBER_PL
import io.nimbly.tzatziki.editor.BOLD
import io.nimbly.tzatziki.editor.ITALIC
import io.nimbly.tzatziki.util.TZATZIKI_NAME
import io.nimbly.tzatziki.util.countMatches
import org.jetbrains.plugins.cucumber.psi.impl.GherkinFeatureHeaderImpl
import java.util.regex.Pattern

private val BOLD_PATTERN = Pattern.compile("[^\\*]*(\\*\\*[^\\*]*\\*\\*)[^\\*]*", Pattern.MULTILINE)
private val STAR_START_PATTERN = Pattern.compile("^[\\s]*(\\*.*$)", Pattern.MULTILINE)

class   TzMarkdownAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        if (!TOGGLE_CUCUMBER_PL)
            return

        if (element !is GherkinFeatureHeaderImpl) return

        //
        // Concatenate text
        val text = element.text

        // Early exit: header has no markdown markers at all → nothing to annotate.
        if ('*' !in text) return

        //
        // BOLD
        val bolds = HashMap<Int, Int>(16)
        var matcher = BOLD_PATTERN.matcher(text)
        while (matcher.find()) {
            ProgressManager.checkCanceled()
            val from = element.textOffset + matcher.start(1)
            val to = element.textOffset + matcher.end(1)
            val r = TextRange(from, to)
            holder.newAnnotation(HighlightSeverity.INFORMATION, TZATZIKI_NAME)
                .range(r).textAttributes(BOLD).create()
            bolds[from] = r.length
        }

        //
        // BULLETS
        val bullets = HashSet<Int>()
        matcher = STAR_START_PATTERN.matcher(text)
        while (matcher.find()) {
            ProgressManager.checkCanceled()
            val group = matcher.group(1)
            if (group.startsWith("**")) continue
            if (group.endsWith("*") && countMatches(group, "*") % 2 == 0) continue
            bullets.add(matcher.start(1))
        }

        //
        // ITALIC
        var i = 0
        var star = -1
        while (i < text.length) {
            if ((i and 0xFFF) == 0) ProgressManager.checkCanceled()
            if (i in bullets) {
                star = -1
                i++
                continue
            }
            val c = text[i]
            if (c == '*') {
                val boldLen = bolds[i]
                if (boldLen != null) {
                    star = -1
                    i += boldLen
                    continue
                }
                star =
                    if (star < 0) {
                        i
                    }
                    else {
                        val from = element.textOffset + star
                        val to = element.textOffset + i
                        val r = TextRange(from, to + 1)
                        holder.newAnnotation(HighlightSeverity.INFORMATION, TZATZIKI_NAME)
                            .range(r).textAttributes(ITALIC).create()
                        -1
                    }
            }
            i++
        }

    }

}