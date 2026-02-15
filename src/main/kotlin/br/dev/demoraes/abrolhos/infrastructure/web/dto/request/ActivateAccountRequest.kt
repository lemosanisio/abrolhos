package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

data class ActivateAccountRequest(
    val inviteToken: String,
    val totpCode: String,
)
