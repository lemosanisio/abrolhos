package br.dev.demoraes.abrolhos.infrastructure.web.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter to enforce strong security headers on all HTTP responses.
 *
 * Implements requirement 4.1 from security recommendations. Applies:
 * - Strict-Transport-Security (HSTS)
 * - X-Content-Type-Options
 * - X-Frame-Options
 * - X-XSS-Protection
 * - Content-Security-Policy
 * - Referrer-Policy
 */
@Component
class SecurityHeadersFilter : OncePerRequestFilter(), Ordered {

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        // Enforce HTTPS-only communication for 1 year, including subdomains
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff")

        // Prevent clickjacking (deny rendering in iframes)
        response.setHeader("X-Frame-Options", "DENY")

        // Enable browser XSS filtering
        response.setHeader("X-XSS-Protection", "1; mode=block")

        // Restrict allowed content sources (basic policy)
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'")

        // Control referrer information sent
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")

        filterChain.doFilter(request, response)
    }

    // Set high precedence to ensure headers are added early
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 2
}
