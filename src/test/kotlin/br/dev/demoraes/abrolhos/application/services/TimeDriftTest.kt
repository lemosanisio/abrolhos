package br.dev.demoraes.abrolhos.application.services

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import org.junit.jupiter.api.Test
import java.util.Date

class TimeDriftTest {

    @Test
    fun testSpecificSecret() {
        val secretStr = "E52A6KFEZOC3SFIBJPX32N6WAW6MD7FO"
        val base32 = Base32()
        val secretBytes = base32.decode(secretStr.uppercase())
        val generator = GoogleAuthenticator(secretBytes)

        // Target time: 2026-02-12 14:22:55 -0300
        // Which is 17:22:55 UTC
        // Unix timestamp for 2026-02-12 17:22:55 UTC is 1770916975000L

        // Let's print codes for a range around this time
        val targetTime = 1770916975000L

        println("--- Generating Codes around 17:22:55 UTC ---")
        println("Secret: $secretStr")

        for (i in -10..10) {
            val offset = i * 30000L
            val time = targetTime + offset
            val date = Date(time)
            val code = generator.generate(date)

            println("Offset: $i ($date) -> Code: $code")
        }
    }
}
