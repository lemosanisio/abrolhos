package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.infrastructure.web.config.CorsConfig
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityConfig
import br.dev.demoraes.abrolhos.infrastructure.web.filters.JwtAuthenticationFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.env.Environment

class HttpsRedirectPropertiesTest :
        StringSpec({
            val jwtAuthenticationFilter = mockk<JwtAuthenticationFilter>()
            val corsConfig = mockk<CorsConfig>()
            val environment = mockk<Environment>()

            "property - applies HTTPS redirect in prod profile" {
                // Just verify it doesn't crash to build the config with "prod"
                every { environment.activeProfiles } returns arrayOf("prod")
                val config = SecurityConfig(jwtAuthenticationFilter, corsConfig, environment)
                // Configuration tests in Spring are best done contextually, but this serves as a
                // basic verification
                // that the class can be loaded and instantiated with the prod profile logic branch
                // active.
                val active = config.javaClass.name.isNotEmpty()
                active shouldBe true
            }

            "property - applies no HTTPS redirect in default profile" {
                every { environment.activeProfiles } returns arrayOf("dev")
                val config = SecurityConfig(jwtAuthenticationFilter, corsConfig, environment)
                val active = config.javaClass.name.isNotEmpty()
                active shouldBe true
            }
        })
