package br.dev.demoraes.abrolhos.domain.authentication.entities

data class Tag(
    val name: String,
    val slug: String,
    val posts: MutableSet<Post>
)
