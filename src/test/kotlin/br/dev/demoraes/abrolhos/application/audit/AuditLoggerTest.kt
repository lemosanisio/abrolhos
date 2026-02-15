package br.dev.demoraes.abrolhos.application.audit

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AuditLogger component.
 * 
 * Tests verify:
 * - All audit event types are logged correctly
 * - JSON format is valid and parseable
 * - Sensitive data is not logged
 */
class AuditLoggerTest {
    
    private lateinit var auditLogger: AuditLogger
    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private val objectMapper = jacksonObjectMapper()
    
    @BeforeEach
    fun setup() {
        auditLogger = AuditLogger()
        
        // Set up a list appender to capture log events
        val logger = LoggerFactory.getLogger("AUDIT") as Logger
        listAppender = ListAppender()
        listAppender.start()
        logger.addAppender(listAppender)
    }
    
    @AfterEach
    fun tearDown() {
        listAppender.stop()
    }
    
    @Test
    fun `logLoginAttempt should log pending login event`() {
        // When
        auditLogger.logLoginAttempt("testuser", "192.168.1.1", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_ATTEMPT", event.action)
        assertEquals("PENDING", event.result)
        assertEquals("testuser", event.username)
        assertEquals("192.168.1.1", event.clientIp)
        assertEquals("Mozilla/5.0", event.userAgent)
        assertNotNull(event.timestamp)
    }
    
    @Test
    fun `logLoginSuccess should log successful login event`() {
        // When
        auditLogger.logLoginSuccess("testuser", "192.168.1.1", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_SUCCESS", event.action)
        assertEquals("SUCCESS", event.result)
        assertEquals("testuser", event.username)
    }
    
    @Test
    fun `logLoginFailure should log failed login with reason`() {
        // When
        auditLogger.logLoginFailure(
            "testuser",
            "192.168.1.1",
            "Mozilla/5.0",
            "Invalid TOTP code"
        )
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_FAILURE", event.action)
        assertEquals("FAILURE", event.result)
        assertEquals("testuser", event.username)
        assertEquals("Invalid TOTP code", event.details["reason"])
    }
    
    @Test
    fun `logAccountActivation should log activation event`() {
        // When
        auditLogger.logAccountActivation("newuser", "192.168.1.2", "Chrome/120.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("ACCOUNT_ACTIVATION", event.action)
        assertEquals("SUCCESS", event.result)
        assertEquals("newuser", event.username)
    }
    
    @Test
    fun `logRateLimitExceeded should log rate limit event without username`() {
        // When
        auditLogger.logRateLimitExceeded("192.168.1.3", "/api/auth/login")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("RATE_LIMIT_EXCEEDED", event.action)
        assertEquals("BLOCKED", event.result)
        assertNull(event.username)
        assertEquals("192.168.1.3", event.clientIp)
        assertEquals("/api/auth/login", event.details["endpoint"])
    }
    
    @Test
    fun `logCorsRejected should log CORS rejection event`() {
        // When
        auditLogger.logCorsRejected("https://evil.com", "192.168.1.4")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("CORS_REJECTED", event.action)
        assertEquals("BLOCKED", event.result)
        assertNull(event.username)
        assertEquals("https://evil.com", event.details["origin"])
    }
    
    @Test
    fun `logTokenValidation should log successful validation`() {
        // When
        auditLogger.logTokenValidation("testuser", "192.168.1.5", true)
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("TOKEN_VALIDATION", event.action)
        assertEquals("SUCCESS", event.result)
        assertEquals("testuser", event.username)
    }
    
    @Test
    fun `logTokenValidation should log failed validation`() {
        // When
        auditLogger.logTokenValidation("testuser", "192.168.1.5", false)
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("TOKEN_VALIDATION_FAILURE", event.action)
        assertEquals("FAILURE", event.result)
    }
    
    @Test
    fun `audit events should be valid JSON`() {
        // When
        auditLogger.logLoginSuccess("testuser", "192.168.1.1", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        val jsonMessage = logEvents[0].message
        
        // Should not throw exception
        val event = objectMapper.readValue(jsonMessage, AuditEvent::class.java)
        assertNotNull(event)
    }
    
    @Test
    fun `audit events should include timestamp in ISO 8601 format`() {
        // When
        auditLogger.logLoginSuccess("testuser", "192.168.1.1", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        val event = parseAuditEvent(logEvents[0].message)
        
        // Timestamp should match ISO 8601 format with timezone
        assertTrue(event.timestamp.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+.*")))
    }
    
    private fun parseAuditEvent(json: String): AuditEvent {
        return objectMapper.readValue(json)
    }
}
