package br.dev.demoraes.abrolhos.infrastructure.web.filters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class SecurityHeadersFilterTest {

    private val filter = SecurityHeadersFilter()

    @Test
    fun `should inject all required security headers`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = MockFilterChain()

        filter.doFilter(request, response, filterChain)

        assertEquals(
                "max-age=31536000; includeSubDomains",
                response.getHeader("Strict-Transport-Security")
        )
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
        assertEquals("DENY", response.getHeader("X-Frame-Options"))
        assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"))
        assertEquals(
                "default-src 'self'; frame-ancestors 'none'",
                response.getHeader("Content-Security-Policy")
        )
        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"))
    }
}
