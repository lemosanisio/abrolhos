package br.dev.demoraes.abrolhos.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date

@Component
class JwtTokenProvider {
    private val issuer = "abrolhos"
    private val secret: String = System.getenv("JWT_SECRET") ?: "dev-secret-change-me"
    private val validitySeconds: Long = (System.getenv("JWT_TTL_SECONDS") ?: "3600").toLong()

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generateToken(
        username: String,
        roles: Collection<String>,
    ): String {
        val now = Instant.now()
        val expiresAt = Date.from(now.plusSeconds(validitySeconds))
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(username)
            .withClaim("roles", roles.toList())
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun validate(token: String): Boolean =
        try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            verifier.verify(token)
            true
        } catch (_: Exception) {
            false
        }

    fun getUsername(token: String): String? =
        try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            val decoded: DecodedJWT = verifier.verify(token)
            decoded.subject
        } catch (_: Exception) {
            null
        }

    fun getRoles(token: String): List<String> =
        try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            val decoded: DecodedJWT = verifier.verify(token)
            decoded.getClaim("roles").asList(String::class.java) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
}
