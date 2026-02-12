package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class TotpCode(@get:JsonValue val value: String) {
    companion object {
        private val TOTP_CODE_REGEX = Regex("^\\d{6}$")
    }

    init {
        require(TOTP_CODE_REGEX.matches(value)) {
            "TOTP code must be exactly 6 digits."
        }
    }

    override fun toString(): String = value
}
