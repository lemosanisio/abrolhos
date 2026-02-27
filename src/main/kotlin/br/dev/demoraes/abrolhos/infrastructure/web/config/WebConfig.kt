
package br.dev.demoraes.abrolhos.infrastructure.web.config

import br.dev.demoraes.abrolhos.infrastructure.web.filters.RateLimitFilter
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web MVC configuration for registering interceptors.
 *
 * Registers the RateLimitFilter to intercept authentication endpoint requests.
 *
 * Requirements: 2.1, 2.2, 2.3, 2.6, 2.7, 2.8
 */
@Configuration
class WebConfig(
    private val rateLimitFilter: RateLimitFilter
) : WebMvcConfigurer {

    /**
     * Registers interceptors with Spring MVC.
     *
     * Requirement 2.6, 2.7, 2.8: Register rate limit filter for auth endpoints
     *
     * @param registry The interceptor registry
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        // Register rate limit filter for authentication endpoints
        registry.addInterceptor(rateLimitFilter)
            .addPathPatterns("/api/auth/**")

        super.addInterceptors(registry)
    }
}
