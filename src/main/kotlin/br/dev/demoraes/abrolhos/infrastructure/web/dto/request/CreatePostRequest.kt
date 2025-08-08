package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

data class CreatePostRequest(
    val title: String,
    val content: String,
    val slug: String,
    val categoryName: String?,
    val tagNames: Set<String>?
)
