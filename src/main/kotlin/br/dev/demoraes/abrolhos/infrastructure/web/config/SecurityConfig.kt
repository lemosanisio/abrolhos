package br.dev.demoraes.abrolhos.infrastructure.web.config

import br.dev.demoraes.abrolhos.infrastructure.web.filters.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
 * ```
 *    - Public endpoints (login, invite, etc.) are allowed.
 *    - All other endpoints require authentication.
 * ```
 * 5. If authorized, the request proceeds to the specific Controller.
 */
@Configuration
@EnableMethodSecurity
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
                // PUT and DELETE on posts require authentication (must come before the permitAll
                // rules)
                authorize(HttpMethod.PUT, "/api/posts/**", authenticated)
                authorize(HttpMethod.DELETE, "/api/posts/**", authenticated)
                authorize("/api/posts", permitAll)
                authorize("/api/posts/**", permitAll)
                authorize("/api/password/reset/request", permitAll)
                authorize("/api/password/reset/confirm", permitAll)
                authorize("/actuator/**", permitAll)
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
