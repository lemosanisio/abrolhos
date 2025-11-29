package br.dev.demoraes.abrolhos.domain.services.commands

data class CreateCategoryCommand(
    val name: String,
)

data class UpdateCategoryCommand(
    val id: String,
    val name: String,
)
