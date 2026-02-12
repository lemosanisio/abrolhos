package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TotpService {
    fun generateSecret(): TotpSecret {
        val random = SecureRandom()
        val bytes = ByteArray(20) // 160 bits
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
            val previousCode = generator.generate(java.util.Date(currentTime - 30000))
            val nextCode = generator.generate(java.util.Date(currentTime + 30000))
            
            code.value == currentCode || 
            code.value == previousCode || 
            code.value == nextCode
        } catch (e: Exception) {
            false
        }
    }
    
    fun generateProvisioningUri(username: String, secret: TotpSecret, issuer: String = "Abrolhos"): String {
        return "otpauth://totp/$issuer:$username?secret=${secret.value}&issuer=$issuer&algorithm=SHA1&digits=6&period=30"
    }
}
