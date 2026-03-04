package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.infrastructure.web.config.CorsConfig
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.forAll
import org.springframework.mock.env.MockEnvironment

class CorsOriginValidationPropertiesTest :
        StringSpec({
            "property - validates well-formed HTTP/HTTPS origins" {
                val validUrlArb =
                        Arb.stringPattern(
                                "(http|https)://[a-z0-9]{3,10}\\.[a-z]{2,3}(:[0-9]{4,5})?"
                        )

                forAll(validUrlArb) { url ->
                    val properties = SecurityProperties().apply { cors.allowedOrigins = url }
                    val environment = MockEnvironment().apply { setActiveProfiles("prod") }
                    val config = CorsConfig(properties, environment)

                    // Should not throw any exception during validation
                    config.validateCorsConfiguration()
                    true
                }
            }

            "property - rejects invalid origins in production" {
                // Arbitrary strings that don't start with http/https
                val invalidUrlArb = Arb.stringPattern("(ftp|ws|file)://[a-z0-9]{3,10}\\.[a-z]{2,3}")

                forAll(invalidUrlArb) { invalidUrl ->
                    val properties =
                            SecurityProperties().apply {
                                cors.allowedOrigins = "$invalidUrl,https://valid-example.com"
                            }
                    val environment = MockEnvironment().apply { setActiveProfiles("prod") }
                    val config = CorsConfig(properties, environment)

                    val exception =
                            shouldThrow<IllegalStateException> {
                                config.validateCorsConfiguration()
                            }
                    exception.message!!.contains("must be http or https") ||
                            exception.message!!.contains("must include a scheme")
                }
            }
        })
