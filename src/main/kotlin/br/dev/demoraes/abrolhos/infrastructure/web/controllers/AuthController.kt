package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.application.services.AuthService
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.PlaintextPassword
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.ActivateAccountRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.LoginRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.AuthResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.InviteValidationResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * REST Controller for authentication-related endpoints.
 *
 * Exposes endpoints for:
 * - Validating invites (public)
 * - Activating accounts with password + TOTP setup (public)
 * - Logging in with username, password, and TOTP (public)
 */
@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController(private val authService: AuthService) {

    @GetMapping("/invite/{token}")
    @ResponseStatus(HttpStatus.OK)
    fun validateInvite(@PathVariable token: String): InviteValidationResponse {
        logger.info("Received request to validate invite token")
        val details = authService.validateInvite(InviteToken(token))
        logger.info("Invite token validated successfully")
        return InviteValidationResponse(details.username, details.provisioningUri)
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.OK)
    fun activateAccount(
            @RequestBody @Valid request: ActivateAccountRequest,
            httpRequest: HttpServletRequest,
    ): AuthResponse {
        logger.info("Received request to activate account")
        val token =
                authService.activateAccount(
                        InviteToken(request.inviteToken),
                        PlaintextPassword(request.password),
                        TotpCode(request.totpCode),
                )
        logger.info("Account activated successfully")
        return AuthResponse(token)
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
            @RequestBody @Valid request: LoginRequest,
            httpRequest: HttpServletRequest,
    ): AuthResponse {
        logger.info("Received login request")
        val clientIp = httpRequest.remoteAddr ?: "unknown"
        val token =
                authService.login(
                        Username(request.username),
                        PlaintextPassword(request.password),
                        TotpCode(request.totpCode),
                        clientIp,
                )
        logger.info("Login successful")
        return AuthResponse(token)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthController::class.java)
    }
}
