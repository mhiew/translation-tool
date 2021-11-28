package com.minhiew.translation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class IOSFileParserTest {
    @Test
    fun `parses localizable strings file`() {
        val fixture = Path.of("src/test/resources/Localizable.strings").toFile()
        val expected = mapOf(
            "ok_button_title" to "OK",
            "send_button_title" to "Send",
            "welcome_placeholder" to "Welcome %@!",
        )

        val actual = IOSFileParser.parse(fixture)
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}