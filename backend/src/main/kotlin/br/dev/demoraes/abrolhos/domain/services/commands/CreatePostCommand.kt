package br.dev.demoraes.abrolhos.domain.services.commands

data class CreatePostCommand(
    val title: String,
    val content: String,
    val categoryName: String?,
    val tagNames: Set<String>?,
)
