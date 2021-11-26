package com.minhiew.translation

import org.apache.commons.text.similarity.LevenshteinDistance
import kotlin.math.absoluteValue
import kotlin.math.max

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
                levenshteinDistance = LevenshteinDistance.getDefaultInstance().apply(androidValue, iosValue).absoluteValue
            )
        }
    }
}

data class StringComparison(
    val key: String,
    val androidValue: String,
    val iosValue: String,
    val isExactMatch: Boolean,
    val isCaseInsensitiveMatch: Boolean,
    val levenshteinDistance: Int
) {
    //calculates how close two strings are based on levenshtein distance
    //a value of 1.0f means the strings match exactly
    //a value of 0.7f means the strings match ~70%
    val levenshteinPercentage: Float by lazy {
        val maxLength = max(androidValue.length, iosValue.length).toFloat()
        1f - (levenshteinDistance.toFloat() / maxLength)
    }

    //treat 10% difference as similar enough
    fun isSimilar(): Boolean = levenshteinPercentage >= 0.9f
}

//returns the keys within this map that do not belong to the other map
fun Map<String, String>.filterForKeysNotPresentIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { !other.containsKey(it) }

fun Map<String, String>.filterForMatchingKeysIn(other: Map<String, String>): Map<String, String> =
    this.filterKeys { other.containsKey(it) }