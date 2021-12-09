package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AndroidTranslationGeneratorTest {
    private val originalXML = """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<resources>
    |    <string name="android_only" translatable="false">wont be overridden</string>
    |    <string name="another_android_only">wont be overridden</string>
    |
    |    <!-- Comments are not stripped -->
    |    <string name="shared_key_1">To Be Replaced 1</string>
    |    <string name="shared_key_2">To Be Replaced 2</string>
    |    <string name="shared_key_3">To Be Replaced 3</string>
    |</resources>""".trimMargin()

    private val differences = listOf(
        StringComparison(key = "shared_key_1", androidValue = "To Be Replaced 1", iosValue = "ios replacement 1"),
        StringComparison(key = "shared_key_2", androidValue = "To Be Replaced 2", iosValue = "ios replacement yay"),
        StringComparison(key = "shared_key_3", androidValue = "To Be Replaced 3", iosValue = "ios values sanitized %@, %d and %s")
    )

    @Nested
    inner class GenerateSynchronizedAndroidXML {
        @Test
        fun `generates an android strings file with replaced ios text copy for differences`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    <string name="android_only" translatable="false">wont be overridden</string>
        |    <string name="another_android_only">wont be overridden</string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">ios replacement 1</string>
        |    <string name="shared_key_2">ios replacement yay</string>
        |    <string name="shared_key_3">ios values sanitized %@, %d and %@</string>
        |</resources>""".trimMargin()

            val actual = AndroidTranslationGenerator.generateSynchronizedAndroidXML(
                xmlString = originalXML,
                differences = differences,
                blockReplacementOnPlaceholderCountMismatch = false,
                replacements = listOf(TextReplacement(target = "%s", replacementValue = "%@"))
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }

        @Test
        fun `blocks updating android when placeholder counts mismatch`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    <string name="android_only" translatable="false">wont be overridden</string>
        |    <string name="another_android_only">wont be overridden</string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">ios replacement 1</string>
        |    <string name="shared_key_2">ios replacement yay</string>
        |    <string name="shared_key_3">To Be Replaced 3</string>
        |</resources>""".trimMargin()

            val actual = AndroidTranslationGenerator.generateSynchronizedAndroidXML(
                xmlString = originalXML,
                differences = differences,
                blockReplacementOnPlaceholderCountMismatch = true,
                replacements = listOf(TextReplacement(target = "%s", replacementValue = "%@"))
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }
    }

    @Nested
    inner class MergeAndroidTranslations {
        @Test
        fun `removes untranslatable text and replaces string values for common keys`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    
        |    <string name="another_android_only"></string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">French Value 1</string>
        |    <string name="shared_key_2">French Value 2</string>
        |    <string name="shared_key_3">French Value 3</string>
        |</resources>""".trimMargin()

            val otherAndroidStrings = mapOf(
                "shared_key_1" to "French Value 1",
                "shared_key_2" to "French Value 2",
                "shared_key_3" to "French Value 3",
            )

            val actual = AndroidTranslationGenerator.mergeAndroidTranslation(
                mainTemplateXML = originalXML,
                otherAndroidStrings = otherAndroidStrings
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }

        @Test
        fun `keys not found in the other locale are set to empty within the main template`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    
        |    <string name="another_android_only"></string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">French Value 1</string>
        |    <string name="shared_key_2">French Value 2</string>
        |    <string name="shared_key_3"></string>
        |</resources>""".trimMargin()

            val otherAndroidStrings = mapOf(
                "shared_key_1" to "French Value 1",
                "shared_key_2" to "French Value 2",
            )

            val actual = AndroidTranslationGenerator.mergeAndroidTranslation(
                mainTemplateXML = originalXML,
                otherAndroidStrings = otherAndroidStrings
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }

        @Test
        fun `extra keys within the other android strings are ignored`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    
        |    <string name="another_android_only"></string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">French Value 1</string>
        |    <string name="shared_key_2">French Value 2</string>
        |    <string name="shared_key_3">French Value 3</string>
        |</resources>""".trimMargin()

            val otherAndroidStrings = mapOf(
                "shared_key_1" to "French Value 1",
                "shared_key_2" to "French Value 2",
                "shared_key_3" to "French Value 3",
                "key_not_in_main" to "wont be used since it's not within the main template"
            )

            val actual = AndroidTranslationGenerator.mergeAndroidTranslation(
                mainTemplateXML = originalXML,
                otherAndroidStrings = otherAndroidStrings
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }


        @Test
        fun `all main template keys are set as empty when merging an empty resource file`() {
            val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    
        |    <string name="another_android_only"></string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1"></string>
        |    <string name="shared_key_2"></string>
        |    <string name="shared_key_3"></string>
        |</resources>""".trimMargin()

            val otherAndroidStrings = emptyMap<String, String>()

            val actual = AndroidTranslationGenerator.mergeAndroidTranslation(
                mainTemplateXML = originalXML,
                otherAndroidStrings = otherAndroidStrings
            )

            assertThat(actual.asXML()).isEqualTo(expected)
        }
    }
}