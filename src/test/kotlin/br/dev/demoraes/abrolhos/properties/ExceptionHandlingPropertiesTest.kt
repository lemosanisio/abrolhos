package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import br.dev.demoraes.abrolhos.application.services.AuthService
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Property-based test for Generic Exception Handler.
 *
 * **Property 2: Generic Exception Handling with Correlation ID** **Validates: Requirements 3.1,
 * 3.2, 3.3, 3.4**
 */
@AutoConfigureMockMvc
class ExceptionHandlingPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var authService: AuthService

    @Autowired private lateinit var mapper: ObjectMapper

    @Test
    fun `property - unhandled exceptions return 500 with correlation ID`() {
        repeat(100) {
            val correlationId = UUID.randomUUID().toString()
            org.slf4j.MDC.put("correlationId", correlationId)

            val request =
                    mapOf(
                            "username" to "user\$it",
                            "password" to "Pass123!",
                            "totpCode" to "123456"
                    )

            // Simulate random unhandled exception types
            val exception =
                    when (it % 3) {
                        0 -> RuntimeException("Random runtime exception \$it")
                        1 -> IllegalStateException("Random illegal state \$it")
                        else -> NullPointerException("Random null pointer \$it")
                    }

            every { authService.login(any(), any(), TotpCode("123456"), any()) } throws exception

            try {
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(request))
                        )
                        .andExpect(status().isInternalServerError)
                        .andExpect(jsonPath("$.message").value("Internal server error"))
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.correlationId").value(correlationId))
            } finally {
                org.slf4j.MDC.clear()
            }
        }
    }
}
