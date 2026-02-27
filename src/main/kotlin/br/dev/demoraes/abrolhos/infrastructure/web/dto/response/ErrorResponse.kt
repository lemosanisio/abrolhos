package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

import java.time.Instant

data class ErrorResponse(
        val message: String,
        val status: Int,
        val correlationId: String? = null,
        val timestamp: Instant = Instant.now()
)
