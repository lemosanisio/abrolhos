package br.dev.demoraes.abrolhos.application.web.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Validation for login requests (temporary until OpenAPI generator models are used).
 */
data class LoginRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val username: String,
    @field:NotBlank
    @field:Size(min = 6, max = 255)
    val password: String,
)
