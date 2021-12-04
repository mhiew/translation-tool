package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidValueSanitizerTest {
    @Test
    fun `sanitize inputs replaces all values regardless of case sensitivity`() {
        val inputString = "This will be replaced %s along with %S but not s or \$s"
        val replacements = listOf(TextReplacement(target = "%s", replacementValue = "%@"))

        val expected = "This will be replaced %@ along with %@ but not s or \$s"

        val actual = AndroidValueSanitizer.sanitizeInput(inputString, replacements)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test default replacements map`() {
        val inputString = "This will be replaced %s but not %d or %f"

        val expected = "This will be replaced %@ but not %d or %f"

        val actual = AndroidValueSanitizer.sanitizeInput(inputString, replacements = listOf(TextReplacement(target = "%s", replacementValue = "%@")))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `positional specifiers are maintained`() {
        val inputString = "This will be replaced %1\$s along with %2\$s"

        val expected = "This will be replaced %1\$@ along with %2\$@"

        val actual = AndroidValueSanitizer.sanitizeInput(
            input = inputString,
            replacements = listOf(
                TextReplacement(target = "%s", replacementValue = "%@"),
                TextReplacement(target = "\$s", replacementValue = "\$@"),
            )
        )

        assertThat(actual).isEqualTo(expected)
    }
}