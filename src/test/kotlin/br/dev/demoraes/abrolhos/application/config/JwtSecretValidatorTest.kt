package br.dev.demoraes.abrolhos.application.config

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class JwtSecretValidatorTest {

    @Test
    fun `should fail when secret length is 31`() {
        val validator = JwtSecretValidator("1234567890123456789012345678901")
        assertFailsWith<IllegalStateException> { validator.validateJwtSecret() }
    }

    @Test
    fun `should succeed when secret length is 32`() {
        val validator = JwtSecretValidator("12345678901234567890123456789012")
        validator.validateJwtSecret() // should not throw exception
    }

    @Test
    fun `should succeed when secret length is 64`() {
        val validator =
            JwtSecretValidator(
                "1234567890123456789012345678901212345678901234567890123456789012"
            )
        validator.validateJwtSecret() // should not throw exception
    }

    @Test
    fun `should fail when secret is empty`() {
        val validator = JwtSecretValidator("")
        assertFailsWith<IllegalStateException> { validator.validateJwtSecret() }
    }
}
