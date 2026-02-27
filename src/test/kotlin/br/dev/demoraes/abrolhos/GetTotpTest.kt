package br.dev.demoraes.abrolhos

import br.dev.demoraes.abrolhos.application.services.EncryptionService
import br.dev.demoraes.abrolhos.application.services.TotpService
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.Date
import org.junit.jupiter.api.Test

class GetTotpTest {

    @Test
    fun printTotp() {
        val properties =
                SecurityProperties().apply {
                    encryption.key = "RG4zLwEeuUgF733x/lyGl3ebjl0xeY+ToNLwrMMTxhs="
                }
        val encryptionService = EncryptionService(properties, SimpleMeterRegistry())
        encryptionService.validateKeys()

        val totpService = TotpService()

        val encrypted =
                "30XtFI6udRP+HUZUDH0p21rnvX7Oy3clGnjr6jqMBJL4grixO3x97NQCsinhLvl1IN+4cu1hbGKArrYT"
        val decrypted = encryptionService.decrypt(encrypted)
        val totpSecret = TotpSecret(decrypted)
        val currentCode = totpService.generateCodesForWindows(totpSecret, Date()).current

        java.io.File("/tmp/totp.txt").writeText(currentCode)
        println("----------------------------------------")
        println("DECRYPTED SECRET: " + decrypted)
        println("CURRENT TOTP CODE: " + currentCode)
        println("----------------------------------------")
    }
}
