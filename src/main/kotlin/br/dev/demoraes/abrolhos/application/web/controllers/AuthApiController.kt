package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.Application.api.AuthApi
import br.dev.demoraes.abrolhos.Application.dto.LoginRequest
import br.dev.demoraes.abrolhos.Application.dto.LoginResponse
import br.dev.demoraes.abrolhos.infrastructure.security.JwtTokenProvider
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
    override fun login(loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        return try {
            val authentication = UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            val authResult = authenticationManager.authenticate(authentication)
            val principal = authResult.principal as UserDetails
            val roles = principal.authorities.map { it.authority }
            val token = jwtTokenProvider.generateToken(principal.username, roles)
            ResponseEntity.ok(LoginResponse(token))
        } catch (ex: AuthenticationException) {
            ResponseEntity.status(401).build()
        }
    }
}
