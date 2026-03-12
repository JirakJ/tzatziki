package io.nimbly.tzatziki.generation

import java.text.Normalizer

private val DIACRITICAL_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

fun String.stripAccents(): String {
    var string = Normalizer.normalize(this, Normalizer.Form.NFD)
    string = DIACRITICAL_MARKS_REGEX.replace(string, "")
    return string
}

fun String.fixName(): String {
    return if (this == "*") return "When" else this
}