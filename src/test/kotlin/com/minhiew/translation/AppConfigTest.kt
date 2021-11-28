package com.minhiew.translation

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AppConfigTest {
    @Test
    fun `parses sample config file`() {
        val fixture = Path.of("src/test/resources/sample.config").toFile()
        val config = ConfigFactory.parseFile(fixture)

        val expected = AppConfig(
            outputDirectory = Path.of("./output"),
            cleanOutputDirectory = true,
            blockReplacementOnPlaceholderCountMismatch = false,
            useMainAndroidFileAsBaseTemplate = false,
            replaceAndroidSourceFile = true,
            main = LocalizationBundle(language = "en", androidFile = Path.of("values/strings.xml"), iosFile = Path.of("Base.lproj/Localizable.strings")),
            localizations = listOf(
                LocalizationBundle(language = "fr", androidFile = Path.of("values-fr/strings.xml"), iosFile = Path.of("fr.lproj/Localizable.strings")),
                LocalizationBundle(language = "fr-CA", androidFile = Path.of("values-fr-rCA/strings.xml"), iosFile = Path.of("fr-CA.lproj/Localizable.strings")),
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
            outputDirectory = Path.of("./output"),
            main = LocalizationBundle(language = "en", androidFile = Path.of("strings.xml"), iosFile = Path.of("Localizable.strings"))
        )

        val actual = config.extract<AppConfig>()

        assertThat(actual).isEqualTo(expected)
        assertThat(actual.localizations).isEqualTo(emptyList<LocalizationBundle>())
        assertThat(actual.blockReplacementOnPlaceholderCountMismatch).isTrue
        assertThat(actual.useMainAndroidFileAsBaseTemplate).isTrue
        assertThat(actual.cleanOutputDirectory).isFalse
        assertThat(actual.replaceAndroidSourceFile).isFalse
    }
}