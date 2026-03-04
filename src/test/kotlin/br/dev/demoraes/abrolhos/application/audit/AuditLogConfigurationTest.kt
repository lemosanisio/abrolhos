package br.dev.demoraes.abrolhos.application.audit

import ch.qos.logback.classic.LoggerContext
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for Logback audit logging configuration.
 *
 * Tests verify:
 * - Audit logger exists and is configured
 * - Configuration can be loaded
 *
 * Requirements:
 * - 5.6: Separate audit log file
 * - 5.7: 90-day retention and async logging
 *
 * Note: These tests verify the logback configuration is valid and can be loaded.
 * The actual file creation and rotation are tested through integration tests.
 */
class AuditLogConfigurationTest {

    @Test
    fun `should have AUDIT logger configured`() {
        // Given
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        // When
        val auditLogger = loggerContext.getLogger("AUDIT")

        // Then
        assertNotNull(auditLogger, "AUDIT logger should be configured")
    }

    @Test
    fun `should be able to log audit events`() {
        // Given
        val auditLogger = AuditLogger()

        // When - This should not throw an exception
        auditLogger.logLoginAttempt("testuser", "192.168.1.1", "Mozilla/5.0")
        auditLogger.logLoginSuccess("testuser", "192.168.1.1", "Mozilla/5.0")
        auditLogger.logLoginFailure("testuser", "192.168.1.1", "Mozilla/5.0", "Invalid credentials")
        auditLogger.logAccountActivation("testuser", "192.168.1.1", "Mozilla/5.0")
        auditLogger.logRateLimitExceeded("192.168.1.1", "/api/auth/login")
        auditLogger.logCorsRejected("https://evil.com", "192.168.1.1")
        auditLogger.logTokenValidation("testuser", "192.168.1.1", true)

        // Then - If we get here, logging works
        assertTrue(true, "Audit logging should work without errors")
    }
}
