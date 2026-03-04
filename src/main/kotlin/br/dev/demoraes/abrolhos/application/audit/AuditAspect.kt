package br.dev.demoraes.abrolhos.application.audit

import br.dev.demoraes.abrolhos.domain.entities.Username
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * AOP Aspect for auditing authentication-related operations.
 *
 * This aspect intercepts calls to AuthService methods and logs audit events for login attempts,
 * successes, failures, and account activations.
 *
 * Requirements:
 * - 5.1: Log login attempts with username, timestamp, IP, and outcome
 * - 5.2: Log account activations
 * - 5.3: Log authentication failures with reason
 */
@Aspect
@Component
class AuditAspect(private val auditLogger: AuditLogger) {

    private val logger = LoggerFactory.getLogger(AuditAspect::class.java)

    /** Audit login attempts, successes, and failures. Requirement 5.1 */
    @Around("execution(* br.dev.demoraes.abrolhos.application.services.AuthService.login(..))")
    fun auditLogin(joinPoint: ProceedingJoinPoint): Any? {
        val args = joinPoint.args
        val username = (args[0] as Username).value
        val request = getHttpRequest()
        val clientIp = extractClientIp(request)
        val userAgent = request?.getHeader("User-Agent") ?: ""

        // Log the attempt
        auditLogger.logLoginAttempt(username, clientIp, userAgent)

        return try {
            val result = joinPoint.proceed()
            // Log success
            auditLogger.logLoginSuccess(username, clientIp, userAgent)
            result
        } catch (e: Exception) {
            // Log failure with reason
            auditLogger.logLoginFailure(username, clientIp, userAgent, e.message ?: "Unknown error")
            throw e
        }
    }

    /** Audit account activation attempts. Requirement 5.2 */
    @Around(
            "execution(* br.dev.demoraes.abrolhos.application.services.AuthService.activateAccount(..))"
    )
    fun auditActivation(joinPoint: ProceedingJoinPoint): Any? {
        val request = getHttpRequest()
        val clientIp = extractClientIp(request)
        val userAgent = request?.getHeader("User-Agent") ?: ""

        return try {
            val result = joinPoint.proceed()

            // Extract username from the invite token (first argument)
            // We need to get the username after successful activation
            // For now, we'll log with a generic identifier
            auditLogger.logAccountActivation("user", clientIp, userAgent)

            result
        } catch (e: Exception) {
            // Activation failed - could log this as well if needed
            throw e
        }
    }

    /** Extract the HTTP request from the current request context. */
    private fun getHttpRequest(): HttpServletRequest? {
        return try {
            val attributes =
                    RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            attributes?.request
        } catch (e: Exception) {
            logger.warn("Could not extract HTTP request from context: ${e.message}")
            null
        }
    }

    /** Extract client IP address securely. */
    private fun extractClientIp(request: HttpServletRequest?): String {
        return request?.remoteAddr ?: "unknown"
    }
}
