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
    val stringComparisons: Map<String, StringComparison> by lazy {
        commonAndroidStrings.entries.associate {
            val key = it.key
            val androidValue: String = it.value
            val iosValue: String = commonIosStrings[key] ?: ""

            key to StringComparison(
                key = key,
                androidValue = androidValue,
                iosValue = iosValue,
                isExactMatch = androidValue == iosValue,
                isCaseInsensitiveMatch = androidValue != iosValue && androidValue.lowercase() == iosValue.lowercase(),
            )
        }
    }

    val differences: List<StringComparison> = stringComparisons.values.filterNot { it.isExactMatch }

    val exactMatches: List<StringComparison> = stringComparisons.values.filter { it.isExactMatch }
}

data class StringComparison(
    val key: String,
    val androidValue: String,
    val iosValue: String,
    val isExactMatch: Boolean,
    val isCaseInsensitiveMatch: Boolean,
)

//returns the keys within this map that do not belong to the other map
fun Map<String, String>.filterForKeysNotPresentIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { !other.containsKey(it) }

fun Map<String, String>.filterForMatchingKeysIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { other.containsKey(it) }