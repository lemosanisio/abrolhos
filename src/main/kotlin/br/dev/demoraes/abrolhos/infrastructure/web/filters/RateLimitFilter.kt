package br.dev.demoraes.abrolhos.infrastructure.web.filters

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.application.services.RateLimitService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val auditLogger: AuditLogger
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val path = request.requestURI

        if (!isAuthEndpoint(path)) {
            return true
        }

        val clientId = extractClientIdentifier(request)

        return try {
            val result = rateLimitService.tryConsume(clientId, path)

            if (result.isAllowed) {
                response.addHeader("X-RateLimit-Limit", result.limit.toString())
                response.addHeader("X-RateLimit-Remaining", result.remaining.toString())
                response.addHeader("X-RateLimit-Reset", result.resetTime.toString())
                
                logger.debug(
                    "Rate limit check passed for client {} on endpoint {}. Remaining: {}/{}",
                    clientId, path, result.remaining, result.limit
                )
                
                true
            } else {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.addHeader("X-RateLimit-Limit", result.limit.toString())
                response.addHeader("X-RateLimit-Remaining", "0")
                response.addHeader("X-RateLimit-Reset", result.resetTime.toString())
                response.addHeader("Retry-After", result.retryAfterSeconds.toString())
                
                response.contentType = "application/json"
                response.writer.write(
                    """{"error":"Too many requests","message":"Rate limit exceeded. Please try again later.","retryAfter":${result.retryAfterSeconds}}"""
                )
                
                logger.warn(
                    "Rate limit exceeded for client {} on endpoint {}. Retry after: {}s",
                    clientId, path, result.retryAfterSeconds
                )
                
                // Requirement 5.4: Log rate limit exceeded event
                auditLogger.logRateLimitExceeded(clientId, path)
                
                false
            }
        } catch (e: Exception) {
            logger.warn(
                "Rate limiting check failed for client {} on endpoint {}, allowing request: {}",
                clientId, path, e.message
            )
            true
        }
    }

    private fun extractClientIdentifier(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (forwardedFor != null) {
            return forwardedFor.split(",").first().trim()
        }
        return request.remoteAddr
    }

    private fun isAuthEndpoint(path: String): Boolean {
        return path == "/api/auth/login" || 
               path == "/api/auth/activate" ||
               path.startsWith("/api/auth/invite/")
    }
}
