package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class InviteToken(@get:JsonValue val value: String) {
    companion object {
        private const val TOKEN_LENGTH = 32
    }

    init {
        require(value.isNotBlank()) { "Invite token cannot be blank." }
        require(value.length >= TOKEN_LENGTH) {
            "Invite token must be at least $TOKEN_LENGTH characters."
        }
    }

    override fun toString(): String = value
}
