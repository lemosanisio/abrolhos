package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for generating JSON Web Tokens (JWT).
 *
 * This service is the final step in the authentication flow. Once a user is verified (by
 * AuthService), this service creates a signed JWT containing the user's identity (claims). This
 * token is then returned to the client to be used for subsequent authenticated requests.
 */
@Service
class TokenService(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration-hours:24}") private val expirationHours: Long,
) {
    fun generateToken(user: User): String {
        val algorithm = Algorithm.HMAC256(jwtSecret)
        val expiresAt = Instant.now().plus(expirationHours, ChronoUnit.HOURS)

        return JWT.create()
            .withSubject(user.id.toString())
            .withClaim("username", user.username.value)
            .withClaim("role", user.role.name)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }
}
