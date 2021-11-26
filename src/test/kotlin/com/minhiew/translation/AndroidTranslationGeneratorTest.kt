package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.StringReader

class AndroidTranslationGeneratorTest {
    private val originalXML = """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<resources>
    |    <string name="android_only" translatable="false">wont be overriden</string>
    |
    |    <!-- Comments are not stripped -->
    |    <string name="shared_key_1">To Be Replaced 1</string>
    |    <string name="shared_key_2">To Be Replaced 2</string>
    |    <string name="shared_key_3">To Be Replaced 2</string>
    |</resources>""".trimMargin()

    private val differences = listOf<StringComparison>(
        generateFakeComparison(key = "shared_key_1", androidValue = "To Be Replaced 1", iosValue = "ios replacement 1"),
        generateFakeComparison(key = "shared_key_2", androidValue = "To Be Replaced 2", iosValue = "ios replacement yay"),
        generateFakeComparison(key = "shared_key_3", androidValue = "To Be Replaced 3", iosValue = "ios values sanitized %@, %d and %s")
    )

    @Test
    fun `generates an android strings file with replaced ios text copy for differences`() {
        val expected = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<resources>
        |    <string name="android_only" translatable="false">wont be overriden</string>
        |
        |    <!-- Comments are not stripped -->
        |    <string name="shared_key_1">ios replacement 1</string>
        |    <string name="shared_key_2">ios replacement yay</string>
        |    <string name="shared_key_3">ios values sanitized %@, %@ and %@</string>
        |</resources>""".trimMargin()

        val actual = AndroidTranslationGenerator.generateFixedAndroidXML(StringReader(originalXML), differences)

        assertThat(actual.asXML()).isEqualTo(expected)
    }

    private fun generateFakeComparison(key: String, androidValue: String, iosValue: String) = StringComparison(
        key = key,
        androidValue = androidValue,
        iosValue = iosValue,
        // the following don't matter at this point
        isExactMatch = false,
        isCaseInsensitiveMatch = false,
        levenshteinDistance = 2
    )
}