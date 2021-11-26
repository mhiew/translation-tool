package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class AndroidFileParserTest {
    @Test
    fun `parses android strings xml file`() {
        val fixture = File("src/test/resources/android-strings.xml".sanitizeFilePath())
        val expected = mapOf(
            "web_link" to "https://www.test.com",
            "app_name" to "My App",
            "ok_button_title" to "OK",
            "welcome_placeholder" to "Welcome %@!",
            "weird_apostrophe" to "We’re excited to to use the weird apostrophe.",
            "at_sign" to "Hey look at the & sign",
            "special_characters" to "Übersetzungen À Paramètres"
        )

        val actual = AndroidFileParser.parse(fixture)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `parses xml where tags span multiple lines`() {
        val fixture = File("src/test/resources/tags-spanning-multiple-lines.xml".sanitizeFilePath())
        val expected = mapOf(
            "singleline" to "Tag on a single line",
            "multiline" to "End tag is on a new line",
        )

        val actual = AndroidFileParser.parse(fixture)
        assertThat(actual).isEqualTo(expected)
    }
}