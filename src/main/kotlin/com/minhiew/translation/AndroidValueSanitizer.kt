package com.minhiew.translation

object AndroidValueSanitizer {
    //currently we always use %@ for all placeholders on android
    //modify this later when we switch to official string formatters
    val defaultReplacements = mapOf(
        "%d" to "%@",
        "%s" to "%@",
        "%f" to "%@",
        "%%" to "%",
    )

    //replaces all the replacement keys with the corresponding value values within the input string
    fun sanitizeInput(input: String, replacement: Map<String, String> = defaultReplacements): String {
        return replacement.entries.fold(input) { acc, (oldValue, newValue) ->
            acc.replace(oldValue, newValue, ignoreCase = true)
        }
    }
}