package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Authentication API endpoints.
 *
 * Tests run against a real PostgreSQL + Redis via Testcontainers, exercising the authentication
 * flow through the full request pipeline.
 */
class AuthApiIntegrationTest : IntegrationTestBase() {

    @Test
    fun `POST login should return 401 for invalid credentials`() {
        val requestBody = mapOf("username" to "nonexistent_user", "totpCode" to "123456")

        val response =
            restTemplate.postForEntity(
                "/api/auth/login",
                jsonEntity(requestBody),
                String::class.java
            )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("Invalid credentials"))
    }

    @Test
    fun `POST login should return 400 for invalid username format`() {
        val requestBody =
            mapOf(
                "username" to "ab", // Too short, Username requires 3-20 chars
                "totpCode" to "123456"
            )

        val response =
            restTemplate.postForEntity(
                "/api/auth/login",
                jsonEntity(requestBody),
                String::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `GET invite should return 400 for non-existent token`() {
        val response =
            restTemplate.getForEntity(
                "/api/auth/invite/nonexistent-token-value",
                String::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `POST activate should return 400 for non-existent invite`() {
        val requestBody =
            mapOf("inviteToken" to "nonexistent-activation-token", "totpCode" to "123456")

        val response =
            restTemplate.postForEntity(
                "/api/auth/activate",
                jsonEntity(requestBody),
                String::class.java
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}
