package br.dev.demoraes.abrolhos.integration

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class ActuatorPrometheusIntegrationTest : IntegrationTestBase() {

        @Autowired private lateinit var mockMvc: MockMvc

        @Test
        @WithMockUser(roles = ["ADMIN"])
        fun `should expose prometheus metrics on actuator endpoint with custom tags`() {
                mockMvc.perform(get("/actuator/prometheus"))
                        .andExpect(status().isOk)
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "application=\"abrolhos\""
                                                )
                                        )
                        )
        }
}
