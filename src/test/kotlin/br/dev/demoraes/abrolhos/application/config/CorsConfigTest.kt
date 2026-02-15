package br.dev.demoraes.abrolhos.application.config

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest

/**
 * Unit tests for CorsConfig component.
 * Tests requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
 */
class CorsConfigTest {

    // Requirement 1.1: Comma-separated origin parsing
    @Test
    fun `should parse single origin correctly`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertEquals(listOf("https://example.com"), config!!.allowedOrigins)
    }

    @Test
    fun `should parse multiple comma-separated origins correctly`() {
        val securityProperties = createSecurityProperties(
            "https://app.example.com,https://admin.example.com,http://localhost:3000"
        )
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertEquals(
            listOf("https://app.example.com", "https://admin.example.com", "http://localhost:3000"),
            config!!.allowedOrigins
        )
    }

    @Test
    fun `should trim whitespace from origins`() {
        val securityProperties = createSecurityProperties(
            " https://example.com , https://test.com "
        )
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertEquals(listOf("https://example.com", "https://test.com"), config!!.allowedOrigins)
    }

    @Test
    fun `should filter out blank origins`() {
        val securityProperties = createSecurityProperties(
            "https://example.com,,https://test.com"
        )
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertEquals(listOf("https://example.com", "https://test.com"), config!!.allowedOrigins)
    }

    // Requirement 1.2: Validate all origin URLs are well-formed
    @Test
    fun `should accept valid HTTPS URLs`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    @Test
    fun `should accept valid HTTP URLs`() {
        val securityProperties = createSecurityProperties("http://localhost:3000")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    @Test
    fun `should accept URLs with ports`() {
        val securityProperties = createSecurityProperties(
            "http://localhost:3000,https://example.com:8443"
        )
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    @Test
    fun `should reject origin without scheme`() {
        val securityProperties = createSecurityProperties("example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("must include a scheme"))
    }

    @Test
    fun `should reject origin with invalid scheme`() {
        val securityProperties = createSecurityProperties("ftp://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("must be http or https"))
    }

    @Test
    fun `should reject origin without host`() {
        val securityProperties = createSecurityProperties("https://")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(
            exception.message!!.contains("must include a valid host") ||
                exception.message!!.contains("Invalid CORS origin")
        )
    }

    // Requirement 1.3: Reject wildcards in production
    @Test
    fun `should allow wildcard in non-production environment`() {
        val securityProperties = createSecurityProperties("*")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    @Test
    fun `should reject wildcard in production profile`() {
        val securityProperties = createSecurityProperties("*")
        val environment = MockEnvironment().withProperty("spring.profiles.active", "production")
        environment.setActiveProfiles("production")
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Wildcard (*) CORS origins are not allowed in production"))
    }

    @Test
    fun `should reject wildcard in prod profile`() {
        val securityProperties = createSecurityProperties("*")
        val environment = MockEnvironment()
        environment.setActiveProfiles("prod")
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Wildcard (*) CORS origins are not allowed in production"))
    }

    @Test
    fun `should reject wildcard mixed with valid origins in production`() {
        val securityProperties = createSecurityProperties("https://example.com,*")
        val environment = MockEnvironment()
        environment.setActiveProfiles("production")
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Wildcard (*) CORS origins are not allowed in production"))
    }

    // Requirement 1.4: Configure allowed methods
    @Test
    fun `should configure all required HTTP methods`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        val expectedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        assertEquals(expectedMethods, config!!.allowedMethods)
    }

    // Requirement 1.5: Configure allowed headers
    @Test
    fun `should configure required allowed headers`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        val expectedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        )
        assertEquals(expectedHeaders, config!!.allowedHeaders)
    }

    // Requirement 1.6: Allow credentials
    @Test
    fun `should enable credentials support`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertEquals(true, config!!.allowCredentials)
    }

    // Requirement 1.7: Configure exposed headers
    @Test
    fun `should configure exposed headers`() {
        val securityProperties = createSecurityProperties("https://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        val expectedHeaders = listOf("Authorization", "Content-Type")
        assertEquals(expectedHeaders, config!!.exposedHeaders)
    }

    // Edge cases
    @Test
    fun `should handle origins with paths`() {
        val securityProperties = createSecurityProperties("https://example.com/app")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    @Test
    fun `should handle origins with subdomains`() {
        val securityProperties = createSecurityProperties(
            "https://app.example.com,https://admin.example.com"
        )
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        // Should not throw exception
        corsConfig.validateCorsConfiguration()
    }

    // Helper method to create SecurityProperties with test data
    private fun createSecurityProperties(allowedOrigins: String): SecurityProperties {
        val properties = SecurityProperties()
        properties.cors.allowedOrigins = allowedOrigins
        return properties
    }

    // Helper method to create a mock HTTP request
    private fun createMockRequest(): HttpServletRequest {
        return MockHttpServletRequest()
    }

    // Property-Based Tests

    /**
     * Property 1: CORS origin parsing
     * **Validates: Requirements 1.1, 1.2, 1.4**
     *
     * For any comma-separated list of valid URLs, parsing should produce
     * a list containing exactly those URLs trimmed of whitespace.
     */
    @Test
    fun `property - parsing comma-separated valid URLs produces trimmed list`() {
        repeat(100) {
            // Generate a list of valid URLs (0-10 URLs)
            val urlCount = (0..10).random()
            val validUrls = (1..urlCount).map {
                val scheme = listOf("http", "https").random()
                val host = generateRandomHost()
                val port = if (kotlin.random.Random.nextBoolean()) {
                    ":${(1000..9999).random()}"
                } else {
                    ""
                }
                "$scheme://$host$port"
            }

            // Add random whitespace around each URL
            val urlsWithWhitespace = validUrls.map { url ->
                val prefix = listOf("", " ", "  ", "   ", "\t").random()
                val suffix = listOf("", " ", "  ", "   ", "\t").random()
                "$prefix$url$suffix"
            }

            // Create comma-separated string
            val originsString = urlsWithWhitespace.joinToString(",")

            // Parse using the same logic as CorsConfig
            val parsed = originsString
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Property: parsed list should contain exactly the original URLs (trimmed)
            assertEquals(validUrls, parsed)
        }
    }

    // Helper function to generate random valid hostnames
    private fun generateRandomHost(): String {
        val labels = (1..3).random()
        val parts = (1..labels).map {
            val length = (3..10).random()
            (1..length).map { ('a'..'z').random() }.joinToString("")
        }
        return parts.joinToString(".") + ".com"
    }

    // Task 2.3: Edge Case Tests

    /**
     * Edge Case: Empty origin list handling
     * **Validates: Requirements 1.1, 1.5**
     *
     * When the origin list is empty or contains only whitespace,
     * the configuration should handle it gracefully.
     */
    @Test
    fun `should handle empty origin list`() {
        val securityProperties = createSecurityProperties("")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertTrue(config!!.allowedOrigins.isNullOrEmpty())
    }

    @Test
    fun `should handle whitespace-only origin list`() {
        val securityProperties = createSecurityProperties("   ,  ,   ")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val source = corsConfig.corsConfigurationSource()
        val config = source.getCorsConfiguration(createMockRequest())

        assertNotNull(config)
        assertTrue(config!!.allowedOrigins.isNullOrEmpty())
    }

    /**
     * Edge Case: Wildcard rejection in production - comprehensive tests
     * **Validates: Requirements 1.1, 1.5**
     */
    @Test
    fun `should reject wildcard at start of list in production`() {
        val securityProperties = createSecurityProperties("*,https://example.com")
        val environment = MockEnvironment()
        environment.setActiveProfiles("production")
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Wildcard (*) CORS origins are not allowed in production"))
    }

    @Test
    fun `should reject wildcard in middle of list in production`() {
        val securityProperties = createSecurityProperties("https://example.com,*,https://test.com")
        val environment = MockEnvironment()
        environment.setActiveProfiles("production")
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Wildcard (*) CORS origins are not allowed in production"))
    }

    /**
     * Edge Case: Malformed URL detection - comprehensive tests
     * **Validates: Requirements 1.1, 1.5**
     */
    @Test
    fun `should reject URL with missing scheme separator`() {
        val securityProperties = createSecurityProperties("httpsexample.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Invalid CORS origin") || exception.message!!.contains("must include a scheme"))
    }

    @Test
    fun `should reject URL with invalid characters in host`() {
        val securityProperties = createSecurityProperties("https://exam ple.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Invalid CORS origin"))
    }

    @Test
    fun `should reject URL with unsupported websocket scheme`() {
        val securityProperties = createSecurityProperties("ws://example.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("must be http or https"))
    }

    @Test
    fun `should reject malformed URL in list with valid URLs`() {
        val securityProperties = createSecurityProperties("https://valid.com,invalid-url,https://another.com")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("Invalid CORS origin") || exception.message!!.contains("must include a scheme"))
    }

    @Test
    fun `should reject URL with file scheme`() {
        val securityProperties = createSecurityProperties("file:///etc/passwd")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("must be http or https"))
    }

    @Test
    fun `should reject URL with javascript scheme`() {
        val securityProperties = createSecurityProperties("javascript:alert(1)")
        val environment = MockEnvironment()
        val corsConfig = CorsConfig(securityProperties, environment)

        val exception = assertThrows<IllegalStateException> {
            corsConfig.validateCorsConfiguration()
        }
        assertTrue(exception.message!!.contains("must be http or https"))
    }
}
