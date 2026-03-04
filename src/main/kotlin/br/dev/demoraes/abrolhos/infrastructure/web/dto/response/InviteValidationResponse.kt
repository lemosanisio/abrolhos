package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

data class InviteValidationResponse(
    val username: String,
    val provisioningUri: String
)
