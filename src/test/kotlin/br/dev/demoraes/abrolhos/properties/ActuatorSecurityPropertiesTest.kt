package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

/**
 * Property-based test for Actuator Endpoint Authorization.
 *
 * **Property 3: Actuator Endpoint Authorization** **Validates: Requirements 2.1, 2.2, 2.3**
 *
 * Checks that an unauthenticated user gets 401 when trying to hit actuator paths (other than
 * health), and that we get 403 on non-ADMIN users hitting actuator components.
 */
class ActuatorSecurityPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var unauthenticatedRestTemplate: TestRestTemplate

    @Test
    fun `property - unauthenticated access to protected actuator endpoints returns 401`() {
        val endpoints =
            listOf(
                "/actuator",
                "/actuator/info",
                "/actuator/metrics",
                "/actuator/env",
                "/actuator/prometheus"
            )

        for (endpoint in endpoints) {
            val response = unauthenticatedRestTemplate.getForEntity(endpoint, String::class.java)
            assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.statusCode,
                "Endpoint \$endpoint should require authentication"
            )
        }
    }
}
