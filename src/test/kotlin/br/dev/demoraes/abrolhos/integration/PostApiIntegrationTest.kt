package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Posts API endpoints.
 *
 * Tests run against a real PostgreSQL database via Testcontainers, exercising the full request
 * pipeline (controller → service → repository → DB).
 */
class PostApiIntegrationTest : IntegrationTestBase() {

    // -------------------------------------------------------------------------
    // GET /api/posts
    // -------------------------------------------------------------------------

    @Test
    fun `GET posts should return 200 for published posts`() {
        val response =
            restTemplate.getForEntity(
                "/api/posts?status=PUBLISHED&page=0&size=10",
                String::class.java
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `GET posts cursor should return 200`() {
        val response = restTemplate.getForEntity("/api/posts/cursor?size=10", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/{slug}
    // -------------------------------------------------------------------------

    @Nested
    inner class GetPostBySlug {

        @Test
        fun `should return 404 for non-existent slug`() {
            val response =
                restTemplate.getForEntity("/api/posts/this-slug-does-not-exist", String::class.java)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }

        @Test
        fun `should return error response with correct shape for non-existent slug`() {
            val response =
                restTemplate.getForEntity("/api/posts/this-slug-does-not-exist", String::class.java)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertNotNull(response.body)

            val body = objectMapper.readTree(response.body)
            assertTrue(body.has("status"), "Error response must have 'status' field")
            assertTrue(body.has("message"), "Error response must have 'message' field")
            assertTrue(body.has("timestamp"), "Error response must have 'timestamp' field")
            assertEquals(HttpStatus.NOT_FOUND.value(), body["status"].asInt())
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/posts — authentication tests
    // -------------------------------------------------------------------------

    @Nested
    inner class PostAuthentication {

        @Test
        fun `POST posts should return 403 when no authentication provided`() {
            val requestBody =
                mapOf(
                    "title" to "Test Post Title",
                    "content" to "Some test content for the post",
                    "status" to "DRAFT",
                    "categoryName" to "test-category",
                    "tagNames" to listOf("test-tag"),
                    "authorUsername" to "testuser"
                )

            val response =
                restTemplate.postForEntity(
                    "/api/posts",
                    jsonEntity(requestBody),
                    String::class.java
                )

            // Without a JWT token, Spring Security should reject the request
            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }

        @Test
        fun `POST posts with expired JWT should be treated as unauthenticated`() {
            val expiredJwt = generateExpiredJwt("testuser", "ADMIN")
            val requestBody =
                mapOf(
                    "title" to "Test Post",
                    "content" to "Content",
                    "status" to "DRAFT",
                    "categoryName" to "test",
                    "tagNames" to listOf<String>(),
                    "authorUsername" to "testuser"
                )

            val response =
                restTemplate.postForEntity(
                    "/api/posts",
                    jsonAuthEntity(requestBody, expiredJwt),
                    String::class.java
                )

            // Expired JWT is silently dropped by JwtAuthenticationFilter — request proceeds
            // without authentication, so Spring Security denies it (403).
            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }

        @Test
        fun `POST posts with malformed JWT should be treated as unauthenticated`() {
            val requestBody =
                mapOf(
                    "title" to "Test Post",
                    "content" to "Content",
                    "status" to "DRAFT",
                    "categoryName" to "test",
                    "tagNames" to listOf<String>(),
                    "authorUsername" to "testuser"
                )

            val response =
                restTemplate.postForEntity(
                    "/api/posts",
                    jsonAuthEntity(requestBody, "this-is-not-a-jwt"),
                    String::class.java
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/posts/{slug} — authentication tests
    // -------------------------------------------------------------------------

    @Nested
    inner class PutAuthentication {

        @Test
        fun `PUT post without authentication should return 403`() {
            val response =
                restTemplate.exchange(
                    "/api/posts/any-slug",
                    HttpMethod.PUT,
                    jsonEntity(mapOf("content" to "updated")),
                    String::class.java
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }

        @Test
        fun `PUT post with expired JWT should return 403`() {
            val expiredJwt = generateExpiredJwt("testuser", "ADMIN")

            val response =
                restTemplate.exchange(
                    "/api/posts/any-slug",
                    HttpMethod.PUT,
                    jsonAuthEntity(mapOf("content" to "updated"), expiredJwt),
                    String::class.java
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/posts/{slug} — authentication tests
    // -------------------------------------------------------------------------

    @Nested
    inner class DeleteAuthentication {

        @Test
        fun `DELETE post without authentication should return 403`() {
            val response =
                restTemplate.exchange(
                    "/api/posts/any-slug",
                    HttpMethod.DELETE,
                    jsonEntity<Any?>(null),
                    String::class.java
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }

        @Test
        fun `DELETE post with expired JWT should return 403`() {
            val expiredJwt = generateExpiredJwt("testuser", "ADMIN")

            val response =
                restTemplate.exchange(
                    "/api/posts/any-slug",
                    HttpMethod.DELETE,
                    jsonAuthEntity<Any?>(null, expiredJwt),
                    String::class.java
                )

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        }
    }
}
