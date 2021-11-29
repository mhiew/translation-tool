package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnalyzerTest {

    @Nested
    inner class FilterForKeysNotPresentIn {
        private val androidText = mapOf(
            "common_key_0" to "android value 1",
            "common_key_1" to "android value 2",
            "uncommon_key" to "android value 3",
        )

        private val iosText = mapOf(
            "common_key_0" to "ios value 1",
            "common_key_1" to "ios value 2",
        )

        @Test
        fun `android has one unique key that is not present in ios`() {
            val expected = mapOf(
                "uncommon_key" to "android value 3"
            )

            val actual = androidText.filterForKeysNotPresentIn(iosText)

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `there are no unique keys within ios that are not present in android`() {
            val expected = emptyMap<String, String>()

            val actual = iosText.filterForKeysNotPresentIn(androidText)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    inner class FilterForMatchingKeysIn {
        private val androidText = mapOf(
            "common_key_0" to "android value 1",
            "common_key_1" to "android value 2",
            "uncommon_key" to "android value 3",
        )

        private val iosText = mapOf(
            "common_key_0" to "ios value 1",
            "common_key_1" to "ios value 2",
        )

        @Test
        fun `returns common keys when comparing android to ios`() {
            val expected = mapOf(
                "common_key_0" to "android value 1",
                "common_key_1" to "android value 2",
            )

            val actual = androidText.filterForMatchingKeysIn(iosText)

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `returns common keys when comparing ios to android`() {
            val expected = mapOf(
                "common_key_0" to "ios value 1",
                "common_key_1" to "ios value 2",
            )

            val actual = iosText.filterForMatchingKeysIn(androidText)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    inner class PlaceholderOccurrence {
        @Test
        fun `no placeholders`() {
            val actual = "there are no placeholder values in this string".placeholderCount()

            assertThat(actual).isEqualTo(0)
        }

        @Test
        fun `empty string has no placeholders`() {
            val actual = "".placeholderCount()

            assertThat(actual).isEqualTo(0)
        }

        @Test
        fun `one placeholder`() {
            val actual = "there is one placeholder %@ within this string".placeholderCount()

            assertThat(actual).isEqualTo(1)
        }

        @Test
        fun `three placeholders`() {
            val actual = "there is are %@%@ three placeholders %@ within this string".placeholderCount()

            assertThat(actual).isEqualTo(3)
        }

        @Test
        fun `different placeholder values are counted`() {
            val actual = "there are %D%F %f %d %s %S 7 placeholders %@ within this string".placeholderCount()

            assertThat(actual).isEqualTo(7)
        }

        @Test
        fun `positional specifiers are counted`() {
            val actual = "there are 4 placeholders %1$@ %3\$s %3\$d %4\$S within this string".placeholderCount()

            assertThat(actual).isEqualTo(4)
        }
    }

    @Nested
    inner class Analyze {
        val androidStrings = mapOf(
            "common_key_0" to "match exactly",
            "common_key_1" to "match case insensitive",
            "common_key_2" to "Slightly different punctuation.",
            "common_key_3" to "indifferent",
            "android_only_key" to "android only"
        )

        val iosStrings = mapOf(
            "common_key_0" to "match exactly",
            "common_key_1" to "match case Insensitive",
            "common_key_2" to "Slightly different punctuation",
            "common_key_3" to "quite different",
            "ios_only_key" to "ios only"
        )

        @Test
        fun `localization contains unique and common strings`() {
            val expected = LocalizationReport(
                uniqueAndroidStrings = mapOf("android_only_key" to "android only"),
                uniqueIosStrings = mapOf("ios_only_key" to "ios only"),
                commonAndroidStrings = mapOf(
                    "common_key_0" to "match exactly",
                    "common_key_1" to "match case insensitive",
                    "common_key_2" to "Slightly different punctuation.",
                    "common_key_3" to "indifferent",
                ),
                commonIosStrings = mapOf(
                    "common_key_0" to "match exactly",
                    "common_key_1" to "match case Insensitive",
                    "common_key_2" to "Slightly different punctuation",
                    "common_key_3" to "quite different",
                )
            )

            val actual = Analyzer.compare(androidStrings, iosStrings)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `check comparison results`() {
            val expected: Map<String, StringComparison> = mapOf(
                "common_key_0" to StringComparison(
                    key = "common_key_0",
                    androidValue = "match exactly",
                    iosValue = "match exactly",
                ),
                "common_key_1" to StringComparison(
                    key = "common_key_1",
                    androidValue = "match case insensitive",
                    iosValue = "match case Insensitive",
                ),
                "common_key_2" to StringComparison(
                    key = "common_key_2",
                    androidValue = "Slightly different punctuation.",
                    iosValue = "Slightly different punctuation",
                ),
                "common_key_3" to StringComparison(
                    key = "common_key_3",
                    androidValue = "indifferent",
                    iosValue = "quite different",
                ),
            )

            val actual = Analyzer.compare(androidStrings, iosStrings).stringComparisons

            assertThat(actual).isEqualTo(expected)
        }
    }
}