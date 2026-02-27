package br.dev.demoraes.abrolhos.infrastructure.web.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER) ?: UUID.randomUUID().toString()

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId)

        // Add it to the response header as well
        response.setHeader(CORRELATION_ID_HEADER, correlationId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
    }
}
