package br.dev.demoraes.abrolhos.infrastructure.web.handlers

import br.dev.demoraes.abrolhos.application.config.SecurityConfig
import br.dev.demoraes.abrolhos.application.services.AuthService
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.exceptions.AccountAlreadyActiveException
import br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidInviteException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.web.controllers.AuthController
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var authService: AuthService

    @MockkBean private lateinit var userRepository: UserRepository

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should handle AuthenticationException with 401 status`() {
        // Given
        val request = mapOf("username" to "testuser", "totpCode" to "123456")
        every { authService.login(Username("testuser"), TotpCode("123456")) } throws
            AuthenticationException("Invalid credentials")

        // When / Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid credentials"))
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun `should handle InvalidInviteException with 400 status`() {
        // Given
        val validToken = "a".repeat(32)
        val request = mapOf("inviteToken" to validToken, "totpCode" to "123456")
        every {
            authService.activateAccount(InviteToken(validToken), TotpCode("123456"))
        } throws InvalidInviteException("Invalid or expired invite token")

        // When / Then
        mockMvc.perform(
            post("/api/auth/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid or expired invite token"))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `should handle AccountAlreadyActiveException with 409 status`() {
        // Given
        val token = "a".repeat(32)
        val request = mapOf("inviteToken" to token, "totpCode" to "123456")
        every { authService.activateAccount(InviteToken(token), TotpCode("123456")) } throws
            AccountAlreadyActiveException("Account is already active")

        // When / Then
        mockMvc.perform(
            post("/api/auth/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Account is already active"))
            .andExpect(jsonPath("$.status").value(409))
    }

    @Test
    fun `should handle InvalidTotpCodeException with 400 status`() {
        // Given
        val token = "a".repeat(32)
        val request = mapOf("inviteToken" to token, "totpCode" to "123456")
        every { authService.activateAccount(InviteToken(token), TotpCode("123456")) } throws
            InvalidTotpCodeException("Invalid TOTP code")

        // When / Then
        mockMvc.perform(
            post("/api/auth/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid TOTP code"))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `should handle IllegalArgumentException with 400 status`() {
        // Given
        val request = mapOf("username" to "testuser", "totpCode" to "123456")
        every { authService.login(Username("testuser"), TotpCode("123456")) } throws
            IllegalArgumentException("TOTP code must be exactly 6 digits.")

        // When / Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("TOTP code must be exactly 6 digits."))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `should return generic error message for null exception message`() {
        // Given
        val request = mapOf("username" to "testuser", "totpCode" to "123456")
        every { authService.login(Username("testuser"), TotpCode("123456")) } throws IllegalArgumentException()

        // When / Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid input"))
            .andExpect(jsonPath("$.status").value(400))
    }
}
