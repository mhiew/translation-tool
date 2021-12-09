package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AndroidFileParserTest {
    @Test
    fun `parses android strings xml file`() {
        val fixture = Path.of("src/test/resources/android-strings.xml").toFile()
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
        val fixture = Path.of("src/test/resources/tags-spanning-multiple-lines.xml").toFile()
        val expected = mapOf(
            "singleline" to "Tag on a single line",
            "multiline" to "End tag is on a new line",
        )

        val actual = AndroidFileParser.parse(fixture)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `xml comments are not stripped during parsing`() {
        val fixture = Path.of("src/test/resources/android-strings.xml").toFile()
        val document = AndroidFileParser.getDocument(fixture)

        val comments = document.selectNodes("//comment()")
        assertThat(comments).hasSize(2)
        assertThat(comments[0].asXML()).isEqualTo("<!-- Comments are not stripped -->")
        assertThat(comments[1].asXML()).isEqualTo("<!-- Second Comment -->")
    }

    @Test
    fun `empty xml tags are kept`() {
        val fixture = Path.of("src/test/resources/android-strings-with-empty.xml").toFile()
        val expected = mapOf(
            "has_value" to "There's text here",
            "empty_key_1" to "",
            "empty_key_2" to ""
        )

        val actual = AndroidFileParser.parse(fixture)
        assertThat(actual).isEqualTo(expected)
    }

}