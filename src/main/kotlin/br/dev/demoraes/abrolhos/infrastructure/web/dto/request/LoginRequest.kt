package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

data class LoginRequest(
    val username: String,
    val totpCode: String
)
