#!/usr/bin/env kotlin

/**
 * Encryption Key Generation Utility
 * 
 * Generates a secure AES-256 encryption key for TOTP secret encryption.
 * The key is Base64-encoded for easy storage in environment variables.
 * 
 * Usage:
 *   kotlin scripts/GenerateEncryptionKey.kt
 * 
 * Output:
 *   SECURITY_ENCRYPTION_KEY=<base64-encoded-key>
 * 
 * Requirement 3.5: Encryption key generation
 */

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator

fun main() {
    println("=".repeat(70))
    println("AES-256 Encryption Key Generator")
    println("=".repeat(70))
    println()
    
    try {
        // Generate a 256-bit AES key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val key = keyGen.generateKey()
        
        // Encode as Base64 for environment variable storage
        val base64Key = Base64.getEncoder().encodeToString(key.encoded)
        
        println("✓ Successfully generated AES-256 encryption key")
        println()
        println("Add this to your environment variables:")
        println("-".repeat(70))
        println("SECURITY_ENCRYPTION_KEY=$base64Key")
        println("-".repeat(70))
        println()
        println("IMPORTANT SECURITY NOTES:")
        println("  • Keep this key secure and never commit it to version control")
        println("  • Store it in a secure secrets manager in production")
        println("  • If you lose this key, encrypted TOTP secrets cannot be recovered")
        println("  • Generate a new key for each environment (dev, staging, prod)")
        println()
        println("Key Details:")
        println("  • Algorithm: AES")
        println("  • Key Size: 256 bits (32 bytes)")
        println("  • Encoding: Base64")
        println("  • Length: ${base64Key.length} characters")
        println()
        
    } catch (e: Exception) {
        System.err.println("✗ Error generating encryption key: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

main()
