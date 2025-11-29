package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.api.AuthApi
import br.dev.demoraes.abrolhos.application.dto.LoginRequest
import br.dev.demoraes.abrolhos.application.dto.LoginResponse
import br.dev.demoraes.abrolhos.infrastructure.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/v1")
class AuthApiController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
) : AuthApi {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthApiController::class.java)
    }

    override fun login(loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        return try {
            val authentication = UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            val authResult = authenticationManager.authenticate(authentication)
            val principal = authResult.principal as UserDetails
            val roles = principal.authorities.map { it.authority }
            val token = jwtTokenProvider.generateToken(principal.username, roles)
            ResponseEntity.ok(LoginResponse(token))
        } catch (ex: AuthenticationException) {
            logger.warn("Authentication failed for user {}: {}", loginRequest.username, ex.message)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
