
package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.infrastructure.configuration.SecurityConfig
import br.dev.demoraes.abrolhos.infrastructure.services.JwtService
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.LoginRequest
import br.dev.demoraes.abrolhos.infrastructure.web.filters.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthenticationController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class)
@TestPropertySource(
    properties = [
        "application.security.jwt.secret-key=c2VjcmV0IGtleSBmb3IgZXhhbXBsZSBiYXNlNjQgZW5jb2RlZCBzZWNyZXQ=",
        "application.security.jwt.expiration=86400000",
        "application.security.jwt.issuer=abrolhos-test"
    ]
)
class AuthenticationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager

    @MockkBean
    private lateinit var userDetailsService: UserDetailsService

    @MockkBean
    private lateinit var jwtService: JwtService

    @Test
    fun `should return jwt token when login is successful`() {
        val loginRequest = LoginRequest("testuser", "password")
        val userDetails = User.withUsername("testuser").password("password").roles("USER").build()
        val token = "test_token"
        val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

        every { authenticationManager.authenticate(any()) } returns authentication
        every { userDetailsService.loadUserByUsername("testuser") } returns userDetails
        every { jwtService.generateToken(userDetails) } returns token

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { value(token) }
        }
    }

    @Test
    fun `should return 403 forbidden when login fails due to bad credentials`() {
        val loginRequest = LoginRequest("wronguser", "wrongpassword")

        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Invalid credentials")

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 403 forbidden when user details service fails`() {
        val loginRequest = LoginRequest("invaliduser", "password")

        every { authenticationManager.authenticate(any()) } throws InternalAuthenticationServiceException("Error loading user")

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
