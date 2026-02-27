package br.dev.demoraes.abrolhos.application.utils

/**
 * Utility for redacting sensitive information like PII, tokens, and secrets from strings. Built for
 * scrubbing log data.
 */
object SensitiveDataRedactor {

    // Regex patterns for detecting sensitive info
    private val jwtPattern = Regex("eyJ[a-zA-Z0-9_-]+\\.eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
    private val emailPattern =
            Regex(
                    "[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*"
            )
    private val tokenPattern = Regex("(?i)(?<=token[=\"':\\s]{1,5})[a-zA-Z0-9_.-]+")
    private val passwordPattern = Regex("(?i)(?<=password[=\"':\\s]{1,5})[^\\s,}\\]\"']+")
    private val secretPattern = Regex("(?i)(?<=secret[=\"':\\s]{1,5})[a-zA-Z0-9+/=_-]+")

    /** Replaces common sensitive patterns (JWT, email, tokens) in the given text with REDACTED. */
    fun redact(text: String): String {
        if (text.isBlank()) return text

        var redacted = text
        redacted = jwtPattern.replace(redacted, "[REDACTED_JWT]")
        redacted = emailPattern.replace(redacted, "[REDACTED_EMAIL]")
        redacted = tokenPattern.replace(redacted, "[REDACTED_TOKEN]")
        redacted = passwordPattern.replace(redacted, "[REDACTED_PASSWORD]")
        redacted = secretPattern.replace(redacted, "[REDACTED_SECRET]")

        return redacted
    }
}
