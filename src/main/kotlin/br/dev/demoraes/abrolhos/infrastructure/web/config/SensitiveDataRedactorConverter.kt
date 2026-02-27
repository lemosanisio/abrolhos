
package br.dev.demoraes.abrolhos.infrastructure.web.config

import br.dev.demoraes.abrolhos.application.utils.SensitiveDataRedactor
import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * Logback converter that automatically redacts sensitive data from log messages.
 * Uses [SensitiveDataRedactor] to mask PII, tokens, and secrets before they are written to logs.
 */
class SensitiveDataRedactorConverter : MessageConverter() {

    override fun convert(event: ILoggingEvent): String {
        val originalMessage = super.convert(event)
        return SensitiveDataRedactor.redact(originalMessage)
    }
}
