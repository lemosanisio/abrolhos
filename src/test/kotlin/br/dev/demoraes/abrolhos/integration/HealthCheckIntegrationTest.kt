package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Actuator health check endpoints.
 *
 * Validates that health probes are correctly configured and respond with the expected status when
 * all dependencies (PostgreSQL, Redis) are available.
 */
class HealthCheckIntegrationTest : IntegrationTestBase() {

    @Test
    fun `health endpoint should return UP`() {
        val response = restTemplate.getForEntity("/actuator/health", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("UP"))
    }

    @Test
    fun `readiness probe should return 200`() {
        val response = restTemplate.getForEntity("/actuator/health/readiness", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `liveness probe should return 200`() {
        val response = restTemplate.getForEntity("/actuator/health/liveness", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
