package br.dev.demoraes.abrolhos.infrastructure.services

import br.dev.demoraes.abrolhos.infrastructure.configuration.properties.JwtProperties
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
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
        return verifyToken(token)
            .map { decodedJwt ->
                (decodedJwt.subject == userDetails.username) && !isTokenExpired(decodedJwt)
            }
            .getOrDefault(false)
    }

    fun extractUsername(token: String): String? {
        return verifyToken(token).getOrNull()?.subject
    }

    private fun verifyToken(token: String): Result<DecodedJWT> {
        return runCatching { verifier.verify(token) }
    }

    private fun isTokenExpired(decodedJWT: DecodedJWT): Boolean {
        return decodedJWT.expiresAt.before(Date())
    }
}
