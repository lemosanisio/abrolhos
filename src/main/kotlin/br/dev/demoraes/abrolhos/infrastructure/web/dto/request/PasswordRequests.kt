package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Request body for changing the authenticated user's password. */
data class ChangePasswordRequest(
        @field:NotBlank(message = "Current password is required") val currentPassword: String,
        @field:NotBlank(message = "New password is required") val newPassword: String,
)

/** Request body for initiating a password reset flow. */
data class PasswordResetRequest(
        @field:NotBlank(message = "Username is required") val username: String,
)

/** Request body for completing a password reset with an issued token. */
data class ConfirmPasswordResetRequest(
        @field:NotBlank(message = "Token is required")
        @field:Size(min = 64, max = 64, message = "Invalid token format")
        val token: String,
        @field:NotBlank(message = "New password is required") val newPassword: String,
)
