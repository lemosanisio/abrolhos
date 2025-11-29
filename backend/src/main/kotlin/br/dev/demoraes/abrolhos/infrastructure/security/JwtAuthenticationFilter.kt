package br.dev.demoraes.abrolhos.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsServiceImpl,
) : OncePerRequestFilter() {
    companion object {
        private const val TOKEN_PREFIX = "Bearer "
        private const val TOKEN_PREFIX_LENGTH = 7
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            val token = header.substring(TOKEN_PREFIX_LENGTH)
            if (jwtTokenProvider.validate(token)) {
                val username = jwtTokenProvider.getUsername(token)
                val roles = jwtTokenProvider.getRoles(token)
                if (username != null && SecurityContextHolder.getContext().authentication == null) {
                    val userDetails = userDetailsService.loadUserByUsername(username)
                    val authorities =
                        roles.map { role ->
                            val name = if (role.startsWith("ROLE_")) role else "ROLE_" + role
                            SimpleGrantedAuthority(name)
                        }
                    val authToken = UsernamePasswordAuthenticationToken(userDetails, null, authorities)
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
