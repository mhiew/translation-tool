package com.minhiew.translation

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class AppConfigTest {
    @Test
    fun `parses sample config file`() {
        val fixture = File("src/test/resources/sample.config".sanitizeFilePath())
        val config = ConfigFactory.parseFile(fixture)

        val expected = AppConfig(
            outputDirectory = File("./output"),
            blockPlaceholderMismatch = false,
            main = LocalizationBundle(language = "en", androidFile = File("values/strings.xml"), iosFile = File("Base.lproj/Localizable.strings")),
            localizations = listOf(
                LocalizationBundle(language = "fr", androidFile = File("values-fr/strings.xml"), iosFile = File("fr.lproj/Localizable.strings")),
                LocalizationBundle(language = "fr-CA", androidFile = File("values-fr-rCA/strings.xml"), iosFile = File("fr-CA.lproj/Localizable.strings")),
            )
        )

        val actual = config.extract<AppConfig>()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `optional properties use default values`() {
        val config = ConfigFactory.parseString(
            """
            |outputDirectory = "./output"
            |main {
            | language = "en"
            | androidFile = "strings.xml"
            | iosFile = "Localizable.strings"
            |}""".trimMargin()
        )

        val expected = AppConfig(
            outputDirectory = File("./output"),
            main = LocalizationBundle(language = "en", androidFile = File("strings.xml"), iosFile = File("Localizable.strings"))
        )

        val actual = config.extract<AppConfig>()

        assertThat(actual).isEqualTo(expected)
        assertThat(actual.localizations).isEqualTo(emptyList<LocalizationBundle>())
        assertThat(actual.blockPlaceholderMismatch).isTrue
    }
}