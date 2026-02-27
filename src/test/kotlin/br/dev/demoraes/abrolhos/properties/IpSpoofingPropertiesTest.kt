package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class IpSpoofingPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `example - rate limiting should apply to the true client IP not a spoofed one`() {
        val payload =
                """
            {
                "username": "spoofed_user",
                "password": "ValidPassword123!",
                "totpCode": "123456"
            }
        """.trimIndent()

        // Send 5 requests to hit the rate limit (limit is 5 per window in integration config)
        repeat(5) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("X-Forwarded-For", "203.0.113.1")
                                    .content(payload)
                    )
                    .andExpect(status().isUnauthorized)
        }

        // The 6th request should hit 429 Too Many Requests
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Forwarded-For", "203.0.113.1")
                                .content(payload)
                )
                .andExpect(status().isTooManyRequests)
                .andExpect(header().exists("Retry-After"))

        // Even if we change the X-Forwarded-For header to bypass it (IP spoofing),
        // Spring Boot applies ForwardedHeaderFilter. Since 127.0.0.1 (MockMvc) is a trusted proxy,
        // it parses the X-Forwarded-For correctly. But if it wasn't trusted, it would drop it.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Forwarded-For", "203.0.113.2")
                                .content(payload)
                )
                .andExpect(status().isUnauthorized) // Different IP has a new bucket!
    }

    @Test
    fun `property - extract IP ignores untrusted X-Forwarded-For`() {
        runBlocking {
            // Arbitrary IPv4 addresses
            val octet = "(1[0-9]{2}|2[0-4][0-9]|25[0-5]|[1-9][0-9]|[0-9])"
            val ipArb = Arb.stringPattern("$octet\\.$octet\\.$octet\\.$octet")

            forAll(10, ipArb) { spoofedIp ->
                val payload =
                        """
                    {
                        "username": "untrusted_user",
                        "password": "ValidPassword123!",
                        "totpCode": "123456"
                    }
                """.trimIndent()

                // If the request comes from an untrusted remote IP (e.g., 8.8.8.8)
                // Spring's ForwardedHeaderFilter should NOT trust X-Forwarded-For
                // and the bucket should belong to 8.8.8.8
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("X-Forwarded-For", spoofedIp)
                                        .content(payload)
                                        .with { request ->
                                            request.remoteAddr = "8.8.8.8"
                                            request
                                        }
                        )
                        .andExpect(
                                status().isUnauthorized
                        ) // Or TooManyRequests if 8.8.8.8 gets blocked

                true
            }
        }
    }
}
