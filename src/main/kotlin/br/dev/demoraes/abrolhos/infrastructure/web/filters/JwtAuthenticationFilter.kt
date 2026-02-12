package br.dev.demoraes.abrolhos.infrastructure.web.filters

import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ulid.ULID

@Component
class JwtAuthenticationFilter(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val algorithm = Algorithm.HMAC256(jwtSecret)
                val verifier = JWT.require(algorithm).build()
                val decodedJWT = verifier.verify(token)

                val userId = decodedJWT.subject
                val role = decodedJWT.getClaim("role").asString()

                val user = userRepository.findById(ULID.parseULID(userId))

                if (user != null && user.isActive) {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            user.username.value,
                            null,
                            authorities
                        )
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (e: JWTVerificationException) {
                log.debug("Invalid JWT token", e)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
