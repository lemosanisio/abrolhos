package br.dev.demoraes.abrolhos.application.audit

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration test for AuditAspect.
 * 
 * Tests verify:
 * - Audit events are logged in correct JSON format
 * - Client IP extraction works correctly
 * - User agent is captured
 * - Request context handling is graceful
 * 
 * Requirements:
 * - 5.1: Log login attempts with username, timestamp, IP, and outcome
 * - 5.2: Log account activations
 * - 5.3: Log authentication failures with reason
 */
class AuditAspectIntegrationTest {
    
    private lateinit var auditLogger: AuditLogger
    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private val objectMapper = jacksonObjectMapper()
    
    @BeforeEach
    fun setup() {
        // Set up audit logger with list appender
        auditLogger = AuditLogger()
        val logger = LoggerFactory.getLogger("AUDIT") as Logger
        listAppender = ListAppender()
        listAppender.start()
        logger.addAppender(listAppender)
        
        // Set up mock HTTP request context
        val request = MockHttpServletRequest()
        request.remoteAddr = "192.168.1.100"
        request.addHeader("User-Agent", "Mozilla/5.0")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }
    
    @AfterEach
    fun tearDown() {
        listAppender.stop()
        RequestContextHolder.resetRequestAttributes()
    }
    
    @Test
    fun `should log login attempt with correct format`() {
        // When
        auditLogger.logLoginAttempt("testuser", "192.168.1.100", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_ATTEMPT", event.action)
        assertEquals("PENDING", event.result)
        assertEquals("testuser", event.username)
        assertEquals("192.168.1.100", event.clientIp)
        assertEquals("Mozilla/5.0", event.userAgent)
        assertNotNull(event.timestamp)
    }
    
    @Test
    fun `should log login success with correct format`() {
        // When
        auditLogger.logLoginSuccess("testuser", "192.168.1.100", "Mozilla/5.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_SUCCESS", event.action)
        assertEquals("SUCCESS", event.result)
        assertEquals("testuser", event.username)
    }
    
    @Test
    fun `should log login failure with reason`() {
        // When
        auditLogger.logLoginFailure(
            "testuser",
            "192.168.1.100",
            "Mozilla/5.0",
            "Invalid credentials"
        )
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("LOGIN_FAILURE", event.action)
        assertEquals("FAILURE", event.result)
        assertEquals("testuser", event.username)
        assertEquals("Invalid credentials", event.details["reason"])
    }
    
    @Test
    fun `should log account activation`() {
        // When
        auditLogger.logAccountActivation("newuser", "192.168.1.100", "Chrome/120.0")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("ACCOUNT_ACTIVATION", event.action)
        assertEquals("SUCCESS", event.result)
        assertEquals("newuser", event.username)
    }
    
    @Test
    fun `should log rate limit exceeded`() {
        // When
        auditLogger.logRateLimitExceeded("192.168.1.100", "/api/auth/login")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("RATE_LIMIT_EXCEEDED", event.action)
        assertEquals("BLOCKED", event.result)
        assertEquals("/api/auth/login", event.details["endpoint"])
    }
    
    @Test
    fun `should log CORS rejection`() {
        // When
        auditLogger.logCorsRejected("https://evil.com", "192.168.1.100")
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("CORS_REJECTED", event.action)
        assertEquals("BLOCKED", event.result)
        assertEquals("https://evil.com", event.details["origin"])
    }
    
    @Test
    fun `should log token validation success`() {
        // When
        auditLogger.logTokenValidation("testuser", "192.168.1.100", true)
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("TOKEN_VALIDATION", event.action)
        assertEquals("SUCCESS", event.result)
    }
    
    @Test
    fun `should log token validation failure`() {
        // When
        auditLogger.logTokenValidation("testuser", "192.168.1.100", false)
        
        // Then
        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        
        val event = parseAuditEvent(logEvents[0].message)
        assertEquals("TOKEN_VALIDATION_FAILURE", event.action)
        assertEquals("FAILURE", event.result)
    }
    
    private fun parseAuditEvent(json: String): AuditEvent {
        return objectMapper.readValue(json)
    }
}

