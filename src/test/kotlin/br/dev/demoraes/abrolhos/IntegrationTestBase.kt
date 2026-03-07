package br.dev.demoraes.abrolhos

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Base class for integration tests using Testcontainers.
 *
 * Provides PostgreSQL 16 and Redis 7 containers, auto-configured datasource and Redis properties,
 * along with helper methods for making authenticated HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
@Tag("integration")
abstract class IntegrationTestBase {

    companion object {
        @Container
        val postgres =
            PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
                withDatabaseName("abrolhos_test")
                withUsername("test")
                withPassword("test")
            }

        @Container
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply { withExposedPorts(6379) }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port", redis::getFirstMappedPort)
        }
    }

    @Autowired protected lateinit var restTemplate: TestRestTemplate

    @Autowired protected lateinit var objectMapper: ObjectMapper

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    /** Creates JSON HTTP headers without authentication. */
    protected fun jsonHeaders(): HttpHeaders {
        return HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
    }

    /** Creates a JSON [HttpEntity] with the given body and no authentication. */
    protected fun <T> jsonEntity(body: T): HttpEntity<T> {
        return HttpEntity(body, jsonHeaders())
    }

    /**
     * Generates a JWT signed with the test secret. The [expiresAt] parameter controls expiry
     * so that both valid and pre-expired tokens can be produced.
     */
    protected fun generateJwt(
        username: String,
        role: String,
        userId: String = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
        expiresAt: Instant = Instant.now().plus(24, ChronoUnit.HOURS),
    ): String {
        val algorithm = Algorithm.HMAC256(jwtSecret)
        return JWT.create()
            .withSubject(userId)
            .withClaim("username", username)
            .withClaim("role", role)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    /** Generates a JWT that is already expired. */
    protected fun generateExpiredJwt(username: String, role: String): String =
        generateJwt(username, role, expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))

    /** Creates [HttpHeaders] with a Bearer token for the given JWT. */
    protected fun authHeaders(jwt: String): HttpHeaders =
        jsonHeaders().apply { setBearerAuth(jwt) }

    /** Creates a JSON [HttpEntity] with the given body and a Bearer auth header. */
    protected fun <T> jsonAuthEntity(body: T, jwt: String): HttpEntity<T> =
        HttpEntity(body, authHeaders(jwt))
}
