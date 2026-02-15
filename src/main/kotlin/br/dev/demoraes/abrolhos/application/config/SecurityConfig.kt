package br.dev.demoraes.abrolhos.application.config

import br.dev.demoraes.abrolhos.infrastructure.web.filters.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Central security configuration for the application.
 *
 * This class defines the security filter chain that processes every incoming HTTP request.
 *
 * Application Flow:
 * 1. Request arrives.
 * 2. `JwtAuthenticationFilter` (registered via `addFilterBefore`) checks for a valid JWT token.
 * 3. `CorsConfig` is applied to handle Cross-Origin Resource Sharing.
 * 4. `authorizeHttpRequests` rules are checked:
 *    - Public endpoints (login, invite, etc.) are allowed.
 *    - All other endpoints require authentication.
 * 5. If authorized, the request proceeds to the specific Controller.
 */
@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsConfig: CorsConfig,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/api/auth/invite/*", permitAll)
                authorize("/api/auth/activate", permitAll)
                authorize("/api/auth/login", permitAll)
                authorize("/api/posts", permitAll)
                authorize("/api/posts/*", permitAll)
                authorize("/error", permitAll)
                authorize(anyRequest, authenticated)
            }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            csrf { disable() }
            cors { configurationSource = corsConfig.corsConfigurationSource() }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }
}
