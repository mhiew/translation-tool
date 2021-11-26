package com.minhiew.translation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidValueSanitizerTest {
    @Test
    fun `sanitize inputs replaces all values regardless of case sensitivity`() {
        val inputString = "This will be replaced %s along with %S but not s or \$s"
        val replacements = mapOf("%s" to "%@")

        val expected = "This will be replaced %@ along with %@ but not s or \$s"

        val actual = AndroidValueSanitizer.sanitizeInput(inputString, replacements)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test default replacements map`() {
        val inputString = "This will be replaced %s along with %d and %f"

        val expected = "This will be replaced %@ along with %@ and %@"

        val actual = AndroidValueSanitizer.sanitizeInput(inputString)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `positional specifiers are maintained`() {
        val inputString = "This will be replaced $1%s along with $2%d"

        val expected = "This will be replaced $1%@ along with $2%@"

        val actual = AndroidValueSanitizer.sanitizeInput(inputString)

        assertThat(actual).isEqualTo(expected)
    }
}