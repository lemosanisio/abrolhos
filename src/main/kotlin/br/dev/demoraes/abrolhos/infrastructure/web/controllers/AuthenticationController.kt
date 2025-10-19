package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.infrastructure.services.JwtService
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.LoginRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.LoginResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): LoginResponse {
        logger.info("Attempting to login user: ${loginRequest.username}")
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
        )

        val userDetails = userDetailsService.loadUserByUsername(loginRequest.username)

        val token = jwtService.generateToken(userDetails)
        logger.info("User ${loginRequest.username} logged in successfully")

        return LoginResponse(token)
    }
}
