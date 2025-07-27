package br.dev.demoraes.abrolhos.domain.authentication.entities

data class Category(
    val name: String,
    val slug: String,
    val posts: MutableSet<Post>
)
