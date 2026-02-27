package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

/** Response body returned when password validation fails. */
data class PasswordValidationErrorResponse(
    val violations: List<String>,
    val status: Int = 400,
)
