package com.minhiew.translation

object Analyzer {
    fun compare(androidStrings: Map<String, String>, iosStrings: Map<String, String>): LocalizationReport {
        return LocalizationReport(
            uniqueAndroidStrings = androidStrings.filterForKeysNotPresentIn(iosStrings),
            uniqueIosStrings = iosStrings.filterForKeysNotPresentIn(androidStrings),
            commonAndroidStrings = androidStrings.filterForMatchingKeysIn(iosStrings),
            commonIosStrings = iosStrings.filterForMatchingKeysIn(androidStrings)
        )
    }
}

data class LocalizationReport(
    val uniqueAndroidStrings: Map<String, String>,
    val uniqueIosStrings: Map<String, String>,
    val commonAndroidStrings: Map<String, String>,
    val commonIosStrings: Map<String, String>,
) {
    //returns the common key to string comparison between ios and android
    val stringComparisons: Map<String, StringComparison> = commonAndroidStrings.entries.associate {
        val key = it.key
        val androidValue: String = it.value
        val iosValue: String = commonIosStrings[key] ?: ""

        key to StringComparison(
            key = key,
            androidValue = androidValue,
            iosValue = iosValue
        )
    }

    val differences: List<StringComparison> = stringComparisons.values.filterNot { it.isExactMatch }

    val exactMatches: List<StringComparison> = stringComparisons.values.filter { it.isExactMatch }

    val caseInsensitiveMatches: List<StringComparison> = stringComparisons.values.filter { it.isCaseInsensitiveMatch }

    val mismatchedPlaceholders: List<StringComparison> = differences.filter { it.hasMismatchedPlaceholders }
}

data class StringComparison(
    val key: String,
    val androidValue: String,
    val iosValue: String,
) {
    val isExactMatch: Boolean = androidValue == iosValue
    val isCaseInsensitiveMatch = !isExactMatch && androidValue.lowercase() == iosValue.lowercase()

    val iosPlaceholderCount: Int = iosValue.placeholderCount()
    val androidPlaceholderCount: Int = androidValue.placeholderCount()
    val hasMismatchedPlaceholders = iosPlaceholderCount != androidPlaceholderCount
}

//returns the keys within this map that do not belong to the other map
fun Map<String, String>.filterForKeysNotPresentIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { !other.containsKey(it) }

fun Map<String, String>.filterForMatchingKeysIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { other.containsKey(it) }


//calculates the total number of occurrences of placeholders
fun String.placeholderCount(): Int = totalOccurrence(input = this, placeholders = setOf("%@", "%d", "%s", "%f", "\$@", "\$d", "\$s", "\$f"))

private fun totalOccurrence(input: String, placeholders: Set<String>): Int {
    return placeholders.fold(initial = 0) { acc, placeholder ->
        acc + input.numberOfOccurrence(placeholder)
    }
}

private fun String.numberOfOccurrence(substring: String): Int {
    var count = 0
    var index = this.indexOf(substring, startIndex = 0, ignoreCase = true)
    while (index > -1) {
        count += 1
        index = this.indexOf(substring, startIndex = index + 1, ignoreCase = true)
    }
    return count
}
