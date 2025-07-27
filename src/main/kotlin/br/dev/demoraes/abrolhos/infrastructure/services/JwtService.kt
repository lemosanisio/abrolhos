package br.dev.demoraes.abrolhos.infrastructure.services

import br.dev.demoraes.abrolhos.infrastructure.configuration.properties.JwtProperties
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    private val jwtProperties: JwtProperties
) {
    private val algorithm: Algorithm = Algorithm.HMAC512(jwtProperties.secretKey)

    private val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(jwtProperties.issuer)
        .build()

    fun generateToken(userDetails: UserDetails): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(jwtProperties.issuer)
            .withSubject(userDetails.username)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + jwtProperties.expiration))
            .withClaim("roles", userDetails.authorities.map { it.authority })
            .sign(algorithm)
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username) && !isTokenExpired(token)
    }

    fun extractUsername(token: String): String? {
        return try {
            val decodedJWT = verifier.verify(token)
            decodedJWT.subject
        } catch (e: JWTVerificationException) {
            null
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val expiration = verifier.verify(token).expiresAt
            expiration.before(Date())
        } catch (e: JWTVerificationException) {
            true
        }
    }
}