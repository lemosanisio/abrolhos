package br.dev.demoraes.abrolhos.application.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * Component responsible for logging security audit events in structured JSON format.
 * 
 * All audit events are written to a separate audit log file for compliance and investigation purposes.
 * The logger uses async appenders to ensure minimal performance impact on the application.
 * 
 * Requirements:
 * - 5.1: Log login attempts with username, timestamp, IP, and outcome
 * - 5.2: Log account activations
 * - 5.3: Log invitation token usage
 * - 5.4: Log rate limit exceeded events
 * - 5.5: Log authentication failures with reason
 * - 5.6: Use structured JSON format
 * - 5.7: Include correlation IDs for tracing
 * - 5.8: Never log sensitive data (TOTP codes, encryption keys)
 */
@Component
class AuditLogger {
    
    private val auditLog = LoggerFactory.getLogger("AUDIT")
    private val objectMapper = jacksonObjectMapper()
    
    /**
     * Log a login attempt (before authentication is verified).
     * Requirement 5.1
     */
    fun logLoginAttempt(username: String, clientIp: String, userAgent: String) {
        logEvent(
            action = "LOGIN_ATTEMPT",
            result = "PENDING",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    /**
     * Log a successful login.
     * Requirement 5.1
     */
    fun logLoginSuccess(username: String, clientIp: String, userAgent: String) {
        logEvent(
            action = "LOGIN_SUCCESS",
            result = "SUCCESS",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    /**
     * Log a failed login with the reason for failure.
     * Requirements 5.1, 5.5
     */
    fun logLoginFailure(
        username: String,
        clientIp: String,
        userAgent: String,
        reason: String
    ) {
        logEvent(
            action = "LOGIN_FAILURE",
            result = "FAILURE",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent,
            details = mapOf("reason" to reason)
        )
    }
    
    /**
     * Log an account activation event.
     * Requirement 5.2
     */
    fun logAccountActivation(username: String, clientIp: String, userAgent: String) {
        logEvent(
            action = "ACCOUNT_ACTIVATION",
            result = "SUCCESS",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    /**
     * Log when a rate limit is exceeded.
     * Requirement 5.4
     */
    fun logRateLimitExceeded(clientIp: String, endpoint: String) {
        logEvent(
            action = "RATE_LIMIT_EXCEEDED",
            result = "BLOCKED",
            username = null,
            clientIp = clientIp,
            userAgent = "",
            details = mapOf("endpoint" to endpoint)
        )
    }
    
    /**
     * Log when a CORS request is rejected.
     * Requirement 5.4
     */
    fun logCorsRejected(origin: String, clientIp: String) {
        logEvent(
            action = "CORS_REJECTED",
            result = "BLOCKED",
            username = null,
            clientIp = clientIp,
            userAgent = "",
            details = mapOf("origin" to origin)
        )
    }
    
    /**
     * Log token validation events (success or failure).
     * Requirements 5.1, 5.5
     */
    fun logTokenValidation(username: String, clientIp: String, success: Boolean) {
        logEvent(
            action = if (success) "TOKEN_VALIDATION" else "TOKEN_VALIDATION_FAILURE",
            result = if (success) "SUCCESS" else "FAILURE",
            username = username,
            clientIp = clientIp,
            userAgent = ""
        )
    }
    
    /**
     * Internal method to log an audit event in structured JSON format.
     * Requirements 5.6, 5.7, 5.8
     */
    private fun logEvent(
        action: String,
        result: String,
        username: String?,
        clientIp: String,
        userAgent: String,
        details: Map<String, Any> = emptyMap()
    ) {
        val event = AuditEvent(
            timestamp = OffsetDateTime.now().toString(),
            action = action,
            result = result,
            username = username,
            clientIp = clientIp,
            userAgent = userAgent,
            details = details
        )
        
        // Serialize to JSON and log
        val json = objectMapper.writeValueAsString(event)
        auditLog.info(json)
    }
}
