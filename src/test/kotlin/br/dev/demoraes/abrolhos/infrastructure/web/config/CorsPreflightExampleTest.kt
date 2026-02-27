package br.dev.demoraes.abrolhos.infrastructure.web.config

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CorsPreflightExampleTest : IntegrationTestBase() {

        @Autowired private lateinit var mockMvc: MockMvc

        @Test
        fun `should accept CORS preflight request for allowed origin`() {
                // Assuming test configuration allows http://localhost:3000 mapping
                mockMvc.perform(
                                options("/api/posts")
                                        .header("Origin", "http://localhost:3000")
                                        .header("Access-Control-Request-Method", "GET")
                        )
                        .andExpect(status().isOk)
                        .andExpect(header().exists("Access-Control-Allow-Origin"))
                        .andExpect(header().exists("Access-Control-Allow-Methods"))
        }

        @Test
        fun `should reject CORS preflight request for disallowed origin if applicable`() {
                // Validates that cross-origin logic is active, although testing denial specifically
                // depends
                // on the exact
                // environment CORS origins value injected during tests. Usually
                // "http://localhost:3000" is
                // allowed.
                mockMvc.perform(
                                options("/api/posts")
                                        .header("Origin", "http://malicious-site.com")
                                        .header("Access-Control-Request-Method", "GET")
                        )
                        // When origin is not matched, standard CORS filter doesn't emit
                        // Access-Control-Allow-Origin
                        .andExpect(
                                status().isForbidden
                        ) // Depends on Spring Security and CORS processing chain
        }
}
