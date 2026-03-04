package br.dev.demoraes.abrolhos.infrastructure.web.filters

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.application.services.RateLimitService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.io.PrintWriter
import java.io.StringWriter

class RateLimitFilterTest {

    private lateinit var rateLimitService: RateLimitService
    private lateinit var auditLogger: AuditLogger
    private lateinit var rateLimitFilter: RateLimitFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var responseWriter: StringWriter
    private lateinit var printWriter: PrintWriter

    @BeforeEach
    fun setup() {
        rateLimitService = mockk()
        auditLogger = mockk(relaxed = true)
        rateLimitFilter = RateLimitFilter(rateLimitService, auditLogger)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        responseWriter = StringWriter()
        printWriter = PrintWriter(responseWriter)

        every { response.writer } returns printWriter
    }

    @Test
    fun `should allow request when under rate limit`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        val result = rateLimitFilter.preHandle(request, response, Any())

        result shouldBe true
        verify { response.addHeader("X-RateLimit-Limit", "5") }
        verify { response.addHeader("X-RateLimit-Remaining", "4") }
        verify { response.addHeader("X-RateLimit-Reset", rateLimitResult.resetTime.toString()) }
    }

    @Test
    fun `should reject request when rate limit exceeded`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = false,
            limit = 5,
            remaining = 0,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 900
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        val result = rateLimitFilter.preHandle(request, response, Any())

        result shouldBe false
        verify { response.status = HttpStatus.TOO_MANY_REQUESTS.value() }
        verify { response.addHeader("X-RateLimit-Limit", "5") }
        verify { response.addHeader("X-RateLimit-Remaining", "0") }
        verify { response.addHeader("X-RateLimit-Reset", rateLimitResult.resetTime.toString()) }
        verify { response.addHeader("Retry-After", "900") }
        verify { response.contentType = "application/json" }
    }

    @Test
    fun `should include retry-after header when rate limited`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = false,
            limit = 5,
            remaining = 0,
            resetTime = System.currentTimeMillis() + 1800000,
            retryAfterSeconds = 1800
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { response.addHeader("Retry-After", "1800") }
    }

    @Test
    fun `should extract client IP from X-Forwarded-For header`() {
        val forwardedIp = "203.0.113.1"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.getHeader("X-Forwarded-For") } returns "$forwardedIp, 10.0.0.1, 10.0.0.2"
        every { request.remoteAddr } returns "10.0.0.3"

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(forwardedIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(forwardedIp, path) }
    }

    @Test
    fun `should fall back to remote address when X-Forwarded-For is not present`() {
        val remoteIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.getHeader("X-Forwarded-For") } returns null
        every { request.remoteAddr } returns remoteIp

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(remoteIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(remoteIp, path) }
    }

    @Test
    fun `should apply rate limiting to login endpoint`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(clientIp, path) }
    }

    @Test
    fun `should apply rate limiting to activate endpoint`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/activate"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(clientIp, path) }
    }

    @Test
    fun `should apply rate limiting to invite endpoints`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/invite/abc123"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(clientIp, path) }
    }

    @Test
    fun `should not apply rate limiting to non-auth endpoints`() {
        val path = "/api/users"

        every { request.requestURI } returns path

        val result = rateLimitFilter.preHandle(request, response, Any())

        result shouldBe true
        verify(exactly = 0) { rateLimitService.tryConsume(any(), any()) }
    }

    @Test
    fun `should fail open when rate limit service throws exception`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null
        every { rateLimitService.tryConsume(clientIp, path) } throws RuntimeException("Redis connection failed")

        val result = rateLimitFilter.preHandle(request, response, Any())

        result shouldBe true
    }

    @Test
    fun `should write JSON error response when rate limited`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = false,
            limit = 5,
            remaining = 0,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 900
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        printWriter.flush()
        val responseBody = responseWriter.toString()
        assert(responseBody.contains("Too many requests"))
        assert(responseBody.contains("\"retryAfter\":900"))
    }

    @Test
    fun `should handle IPv6 addresses`() {
        val ipv6Address = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns ipv6Address
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(ipv6Address, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(ipv6Address, path) }
    }

    @Test
    fun `should trim whitespace from X-Forwarded-For IP`() {
        val forwardedIp = "203.0.113.1"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.getHeader("X-Forwarded-For") } returns "  $forwardedIp  , 10.0.0.1"
        every { request.remoteAddr } returns "10.0.0.2"

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(forwardedIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        verify { rateLimitService.tryConsume(forwardedIp, path) }
    }

    @Test
    fun `should log audit event when rate limit exceeded`() {
        // Requirement 5.4: Log rate limit exceeded events
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = false,
            limit = 5,
            remaining = 0,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 900
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        // Verify audit event was logged
        verify { auditLogger.logRateLimitExceeded(clientIp, path) }
    }

    @Test
    fun `should not log audit event when rate limit not exceeded`() {
        val clientIp = "192.168.1.100"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.remoteAddr } returns clientIp
        every { request.getHeader("X-Forwarded-For") } returns null

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = true,
            limit = 5,
            remaining = 4,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 0
        )
        every { rateLimitService.tryConsume(clientIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        // Verify audit event was NOT logged
        verify(exactly = 0) { auditLogger.logRateLimitExceeded(any(), any()) }
    }

    @Test
    fun `should log audit event with correct client IP from X-Forwarded-For`() {
        val forwardedIp = "203.0.113.1"
        val path = "/api/auth/login"

        every { request.requestURI } returns path
        every { request.getHeader("X-Forwarded-For") } returns "$forwardedIp, 10.0.0.1"
        every { request.remoteAddr } returns "10.0.0.2"

        val rateLimitResult = RateLimitService.RateLimitResult(
            isAllowed = false,
            limit = 5,
            remaining = 0,
            resetTime = System.currentTimeMillis() + 900000,
            retryAfterSeconds = 900
        )
        every { rateLimitService.tryConsume(forwardedIp, path) } returns rateLimitResult

        rateLimitFilter.preHandle(request, response, Any())

        // Verify audit event was logged with the forwarded IP
        verify { auditLogger.logRateLimitExceeded(forwardedIp, path) }
    }
}
