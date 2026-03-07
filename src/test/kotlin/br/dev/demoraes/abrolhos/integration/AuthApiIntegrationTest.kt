package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Nested
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

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    @Nested
    inner class Login {

        @Test
        fun `should return 401 for invalid credentials`() {
            val requestBody =
                mapOf(
                    "username" to "nonexistent_user",
                    "password" to "SomeP@ssw0rd!",
                    "totpCode" to "123456"
                )

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
        fun `should return error response with correct shape for invalid credentials`() {
            val requestBody =
                mapOf(
                    "username" to "nonexistent_user",
                    "password" to "SomeP@ssw0rd!",
                    "totpCode" to "123456"
                )

            val response =
                restTemplate.postForEntity(
                    "/api/auth/login",
                    jsonEntity(requestBody),
                    String::class.java
                )

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertNotNull(response.body)

            val body = objectMapper.readTree(response.body)
            assertTrue(body.has("status"), "Error response must have 'status' field")
            assertTrue(body.has("message"), "Error response must have 'message' field")
            assertTrue(body.has("timestamp"), "Error response must have 'timestamp' field")
            assertEquals(HttpStatus.UNAUTHORIZED.value(), body["status"].asInt())
        }

        @Test
        fun `should return 400 for invalid username format`() {
            val requestBody =
                mapOf(
                    "username" to "ab", // Too short, Username requires 3-20 chars
                    "password" to "SomeP@ssw0rd!",
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
        fun `should return 400 when password is missing`() {
            val requestBody = mapOf("username" to "testuser", "totpCode" to "123456")

            val response =
                restTemplate.postForEntity(
                    "/api/auth/login",
                    jsonEntity(requestBody),
                    String::class.java
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/invite/{token}
    // -------------------------------------------------------------------------

    @Nested
    inner class Invite {

        @Test
        fun `should return 400 for non-existent token`() {
            val response =
                restTemplate.getForEntity(
                    "/api/auth/invite/nonexistent-token-value",
                    String::class.java
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertNotNull(response.body)
        }

        @Test
        fun `should return error response with correct shape for non-existent invite token`() {
            val response =
                restTemplate.getForEntity(
                    "/api/auth/invite/nonexistent-token-value",
                    String::class.java
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertNotNull(response.body)

            val body = objectMapper.readTree(response.body)
            assertTrue(body.has("status"), "Error response must have 'status' field")
            assertTrue(body.has("message"), "Error response must have 'message' field")
            assertTrue(body.has("timestamp"), "Error response must have 'timestamp' field")
            assertEquals(HttpStatus.BAD_REQUEST.value(), body["status"].asInt())
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/activate
    // -------------------------------------------------------------------------

    @Nested
    inner class Activate {

        @Test
        fun `should return 400 for non-existent invite`() {
            val requestBody =
                mapOf(
                    "inviteToken" to "nonexistent-activation-token",
                    "password" to "SomeP@ssw0rd!",
                    "totpCode" to "123456"
                )

            val response =
                restTemplate.postForEntity(
                    "/api/auth/activate",
                    jsonEntity(requestBody),
                    String::class.java
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/password/reset
    // -------------------------------------------------------------------------

    @Nested
    inner class PasswordReset {

        @Test
        fun `reset request should return 202 for any username`() {
            val requestBody = mapOf("username" to "anyuser")

            val response =
                restTemplate.postForEntity(
                    "/api/password/reset/request",
                    jsonEntity(requestBody),
                    String::class.java
                )

            // Always returns 202 to prevent user enumeration
            assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        }

        @Test
        fun `reset confirm should return 400 for invalid token`() {
            val requestBody = mapOf("token" to "a".repeat(64), "newPassword" to "NewSup3rS@fe!")

            val response =
                restTemplate.postForEntity(
                    "/api/password/reset/confirm",
                    jsonEntity(requestBody),
                    String::class.java
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }
}
