package br.dev.demoraes.abrolhos

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
}
