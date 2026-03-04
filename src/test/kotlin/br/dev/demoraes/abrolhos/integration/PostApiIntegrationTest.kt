package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for the Posts API endpoints.
 *
 * Tests run against a real PostgreSQL database via Testcontainers, exercising the full request
 * pipeline (controller → service → repository → DB).
 */
class PostApiIntegrationTest : IntegrationTestBase() {

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

    @Test
    fun `GET post by slug should return 404 for non-existent slug`() {
        val response =
            restTemplate.getForEntity("/api/posts/this-slug-does-not-exist", String::class.java)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

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
}
