package br.dev.demoraes.abrolhos.application.audit

/**
 * Represents a security audit event with all required fields for compliance and investigation.
 *
 * @property timestamp ISO 8601 timestamp with timezone
 * @property action The type of action being audited (e.g., LOGIN_ATTEMPT, RATE_LIMIT_EXCEEDED)
 * @property result The outcome of the action (SUCCESS, FAILURE, BLOCKED, PENDING)
 * @property username The username involved in the action (null for unauthenticated events)
 * @property clientIp The IP address of the client making the request
 * @property userAgent The User-Agent header from the request
 * @property details Additional context-specific information
 */
data class AuditEvent(
    val timestamp: String,
    val action: String,
    val result: String,
    val username: String?,
    val clientIp: String,
    val userAgent: String,
    val details: Map<String, Any> = emptyMap()
)
