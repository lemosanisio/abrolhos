package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
/**
 * Value class for TOTP codes.
 *
 * Ensures the code is exactly 6 digits, preventing invalid formats from propagating through the
 * system.
 */
value class TotpCode(@get:JsonValue val value: String) {
    companion object {
        private val TOTP_CODE_REGEX = Regex("^\\d{6}$")
    }

    init {
        require(TOTP_CODE_REGEX.matches(value)) { "TOTP code must be exactly 6 digits." }
    }

    override fun toString(): String = value
}
