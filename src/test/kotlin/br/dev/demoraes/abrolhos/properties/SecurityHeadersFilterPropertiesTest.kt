package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.infrastructure.web.filters.SecurityHeadersFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class SecurityHeadersFilterPropertiesTest :
        StringSpec({
            val filter = SecurityHeadersFilter()

            "property - applies security headers to responses" {
                forAll(Arb.string()) { uri ->
                    val request = MockHttpServletRequest("GET", uri)
                    val response = MockHttpServletResponse()
                    val chain = MockFilterChain()

                    filter.doFilter(request, response, chain)

                    response.getHeader("Strict-Transport-Security") shouldBe
                            "max-age=31536000; includeSubDomains"
                    response.getHeader("X-Content-Type-Options") shouldBe "nosniff"
                    response.getHeader("X-Frame-Options") shouldBe "DENY"
                    response.getHeader("X-XSS-Protection") shouldBe "1; mode=block"
                    response.getHeader("Content-Security-Policy") shouldBe
                            "default-src 'self'; frame-ancestors 'none'"
                    response.getHeader("Referrer-Policy") shouldBe "strict-origin-when-cross-origin"
                    true
                }
            }
        })
