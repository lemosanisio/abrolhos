package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import br.dev.demoraes.abrolhos.infrastructure.web.filters.CorrelationIdFilter
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Property-based test for Correlation ID Filter.
 *
 * **Property 1: Correlation ID Lifecycle** **Validates: Requirement 2.1 (Observability)**
 */
@AutoConfigureMockMvc
class CorrelationIdFilterPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `property - correlation ID is generated if missing and returned in response`() {
        repeat(50) {
            val mvcResult =
                    mockMvc.perform(get("/actuator/health"))
                            .andExpect(status().isOk)
                            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                            .andReturn()

            val correlationId =
                    mvcResult.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
            assertTrue(
                    correlationId != null && correlationId.isNotEmpty(),
                    "Correlation ID should not be null or empty"
            )

            // Verify it's a valid UUID
            try {
                UUID.fromString(correlationId)
            } catch (e: IllegalArgumentException) {
                org.junit.jupiter.api.fail(
                        "Generated Correlation ID is not a valid UUID: \$correlationId"
                )
            }
        }
    }

    @Test
    fun `property - missing correlation ID from request is preserved in response`() {
        repeat(50) {
            val suppliedCorrelationId = "custom-id-xyz-\$it"

            mockMvc.perform(
                            get("/actuator/health")
                                    .header(
                                            CorrelationIdFilter.CORRELATION_ID_HEADER,
                                            suppliedCorrelationId
                                    )
                    )
                    .andExpect(status().isOk)
                    .andExpect(
                            header().string(
                                            CorrelationIdFilter.CORRELATION_ID_HEADER,
                                            suppliedCorrelationId
                                    )
                    )
        }
    }
}
