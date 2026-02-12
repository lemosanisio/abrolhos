package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.application.services.AuthService
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.Username
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class ActivateAccountRequest(
    val inviteToken: String,
    val totpCode: String,
    val secret: String,
)

data class LoginRequest(
    val username: String,
    val totpCode: String,
)

data class AuthResponse(
    val token: String,
)

data class InviteValidationResponse(
    val username: String,
    val secret: String,
    val provisioningUri: String,
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @GetMapping("/invite/{token}")
    @ResponseStatus(HttpStatus.OK)
    fun validateInvite(@PathVariable token: String): InviteValidationResponse {
        logger.info("Received request to validate invite token")
        val details = authService.validateInvite(InviteToken(token))
        logger.info("Invite token validated successfully for username: {}", details.username)
        return InviteValidationResponse(
            username = details.username,
            secret = details.secret,
            provisioningUri = details.provisioningUri
        )
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.OK)
    fun activateAccount(@RequestBody request: ActivateAccountRequest): AuthResponse {
        logger.info("Received request to activate account with invite token")
        val token =
            authService.activateAccount(
                inviteToken = InviteToken(request.inviteToken),
                totpCode = TotpCode(request.totpCode),
                secret = TotpSecret(request.secret)
            )
        logger.info("Account activated successfully")
        return AuthResponse(token)
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(@RequestBody request: LoginRequest): AuthResponse {
        logger.info("Received login request for username: {}", request.username)
        val token = authService.login(Username(request.username), TotpCode(request.totpCode))
        logger.info("Login successful for username: {}", request.username)
        return AuthResponse(token)
    }
}
