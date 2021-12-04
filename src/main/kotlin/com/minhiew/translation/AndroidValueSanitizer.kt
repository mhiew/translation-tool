package com.minhiew.translation

object AndroidValueSanitizer {
    //replaces all the replacement keys with the corresponding value values within the input string
    fun sanitizeInput(input: String, replacements: List<TextReplacement>): String {
        return replacements.fold(input) { acc, placeholderReplacement ->
            acc.replace(placeholderReplacement.target, placeholderReplacement.replacementValue, ignoreCase = true)
        }
    }
}