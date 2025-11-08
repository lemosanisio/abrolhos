package br.dev.demoraes.abrolhos.application.web.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Validation for incoming create post requests (temporary until OpenAPI generator models are used).
 */
data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    @field:NotBlank
    val content: String,
    @field:Size(max = 100)
    val categoryName: String?,
    val tagNames: Set<
        @Pattern(regexp = "^[a-z0-9_-]{1,50}$")
        String,
        >?,
)
