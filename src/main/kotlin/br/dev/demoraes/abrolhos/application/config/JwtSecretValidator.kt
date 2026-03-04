package br.dev.demoraes.abrolhos.application.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtSecretValidator(@Value("\${jwt.secret}") private val jwtSecret: String) {
    private val log = LoggerFactory.getLogger(JwtSecretValidator::class.java)

    @PostConstruct
    fun validateJwtSecret() {
        if (jwtSecret.length < 32) {
            val message =
                "JWT secret must be at least 32 characters long. Current length: ${jwtSecret.length}"
            log.error(message)
            throw IllegalStateException(message)
        }
        log.info("JWT secret validation passed. Secret length is sufficient.")
    }

    companion object {
        private const val MIN_SECRET_LENGTH = 32
    }
}
