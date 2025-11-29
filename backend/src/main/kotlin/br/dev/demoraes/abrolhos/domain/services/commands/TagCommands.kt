package br.dev.demoraes.abrolhos.domain.services.commands

data class CreateTagCommand(
    val name: String,
)

data class UpdateTagCommand(
    val id: String,
    val name: String,
)
