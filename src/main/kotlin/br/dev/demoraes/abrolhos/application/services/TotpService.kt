package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TotpService {
    private val logger = LoggerFactory.getLogger(TotpService::class.java)

    companion object {
        private const val SECRET_BYTE_SIZE = 20 // 160 bits
        private const val TIME_WINDOW_MILLIS = 30_000L
    }

    fun generateSecret(): TotpSecret {
        val random = SecureRandom()
        val bytes = ByteArray(SECRET_BYTE_SIZE)
        random.nextBytes(bytes)
        val base32 = Base32()
        val secret = base32.encodeToString(bytes).replace("=", "")
        return TotpSecret(secret)
    }

    fun verifyCode(secret: TotpSecret, code: TotpCode): Boolean {
        return try {
            val base32 = Base32()
            val secretBytes = base32.decode(secret.value)
            val generator = GoogleAuthenticator(secretBytes)

            // Check current time window and ±1 window for clock skew
            val currentTime = System.currentTimeMillis()
            val currentCode = generator.generate()
            val previousCode = generator.generate(java.util.Date(currentTime - TIME_WINDOW_MILLIS))
            val nextCode = generator.generate(java.util.Date(currentTime + TIME_WINDOW_MILLIS))

            code.value == currentCode || code.value == previousCode || code.value == nextCode
        } catch (e: IllegalArgumentException) {
            logger.warn("TOTP verification failed due to invalid input: {}", e.message)
            false
        }
    }

    fun generateProvisioningUri(
        username: String,
        secret: TotpSecret,
        issuer: String = "Abrolhos"
    ): String {
        return "otpauth://totp/$issuer:$username?secret=${secret.value}&issuer=$issuer&algorithm=SHA1&digits=6&period=30"
    }
}
