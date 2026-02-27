package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ActivateAccountRequest(
        @field:NotBlank(message = "Invite token is required") val inviteToken: String,
        @field:NotBlank(message = "Password is required") val password: String,
        @field:Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
        val totpCode: String,
)
