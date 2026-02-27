package br.dev.demoraes.abrolhos.infrastructure.web.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.net.URI

/**
 * CORS configuration with environment-based origin validation.
 *
 * This configuration:
 * - Parses comma-separated origins from environment variables
 * - Validates all origins are well-formed URLs
 * - Rejects wildcards in production profile
 * - Configures allowed methods and headers
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
 */
@Configuration
class CorsConfig(
    private val securityProperties: SecurityProperties,
    private val environment: Environment
) {

    private val logger = LoggerFactory.getLogger(CorsConfig::class.java)

    @PostConstruct
    fun validateCorsConfiguration() {
        val origins = parseOrigins()
        val isProduction = isProductionProfile()

        logger.info("Validating CORS configuration (production: $isProduction)")

        // Requirement 1.2: Validate all origins are well-formed URLs
        origins.forEach { origin -> validateOriginUrl(origin) }

        // Requirement 1.3: Reject wildcards in production
        if (isProduction && origins.any { it == "*" }) {
            throw IllegalStateException(
                "Wildcard (*) CORS origins are not allowed in production profile. " +
                    "Please configure specific origins in SECURITY_CORS_ALLOWED_ORIGINS"
            )
        }

        logger.info(
            "CORS configuration validated successfully: ${origins.size} origin(s) configured"
        )
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = parseOrigins()
        val configuration = CorsConfiguration()

        // Requirement 1.1: Parse comma-separated origins
        configuration.allowedOrigins = origins

        // Requirement 1.4: Configure allowed methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")

        // Requirement 1.5: Configure allowed headers
        configuration.allowedHeaders =
            listOf("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")

        // Requirement 1.6: Allow credentials for authenticated requests
        configuration.allowCredentials = true

        // Requirement 1.7: Configure exposed headers
        configuration.exposedHeaders = listOf("Authorization", "Content-Type")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        logger.info("CORS configuration source created with origins: $origins")

        return source
    }

    /**
     * Parse comma-separated origins from configuration. Requirement 1.1: Comma-separated origin
     * parsing
     */
    private fun parseOrigins(): List<String> {
        val originsString = securityProperties.cors.allowedOrigins
        return originsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * Validate that an origin is a well-formed URL or wildcard. Requirement 1.2: Validate all
     * origin URLs are well-formed
     */
    private fun validateOriginUrl(origin: String) {
        // Allow wildcard for non-production environments
        if (origin == "*") {
            return
        }

        try {
            val uri = URI.create(origin)

            // Validate scheme is present and is http or https
            require(!uri.scheme.isNullOrBlank()) {
                "Origin must include a scheme (http:// or https://): $origin"
            }

            require(uri.scheme in listOf("http", "https")) {
                "Origin scheme must be http or https: $origin"
            }

            // Validate host is present and not empty
            // Note: URI.create("https://") results in host being null
            require(!uri.host.isNullOrBlank()) { "Origin must include a valid host: $origin" }
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "Invalid CORS origin URL '$origin': ${e.message}. " +
                    "Origins must be well-formed URLs (e.g., https://example.com)",
                e
            )
        }
    }

    /**
     * Check if the application is running in production profile. Requirement 1.3: Production
     * profile detection
     */
    private fun isProductionProfile(): Boolean {
        val activeProfiles = environment.activeProfiles
        return activeProfiles.contains("production") || activeProfiles.contains("prod")
    }
}
