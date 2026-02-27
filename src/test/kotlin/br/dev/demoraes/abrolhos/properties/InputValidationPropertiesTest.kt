package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.LoginRequest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class InputValidationPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `property - validates domain primitive constraints for Posts`() {
        runBlocking {
            val invalidTitleArb = Arb.string(0..2)

            forAll(invalidTitleArb) { invalidTitle ->
                val payload =
                        """
                    {
                        "title": "$invalidTitle",
                        "content": "Valid content length",
                        "status": "DRAFT",
                        "categoryName": "General",
                        "tagNames": ["tag1"],
                        "authorUsername": "testuser"
                    }
                """.trimIndent()

                mockMvc.perform(
                                post("/api/posts")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload)
                        )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.message").isString)

                true
            }
        }
    }

    @Test
    fun `property - validates @Valid DTO constraints for Auth`() {
        runBlocking {
            val invalidUsernameArb = Arb.string(0..0)

            forAll(10, invalidUsernameArb) { invalidUsername ->
                val loginRequest =
                        LoginRequest(
                                username = invalidUsername,
                                password = "ValidPassword123!",
                                totpCode = "123456"
                        )

                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(loginRequest))
                        )
                        .andExpect(status().isBadRequest)

                true
            }
        }
    }
}
