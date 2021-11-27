package com.minhiew.translation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class IOSFileParserTest {
    @Test
    fun `parses localizable strings file`() {
        val fixture = File("src/test/resources/Localizable.strings".sanitizeFilePath())
        val expected = mapOf(
            "ok_button_title" to "OK",
            "send_button_title" to "Send",
            "welcome_placeholder" to "Welcome %@!",
        )

        val actual = IOSFileParser.parse(fixture)
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}