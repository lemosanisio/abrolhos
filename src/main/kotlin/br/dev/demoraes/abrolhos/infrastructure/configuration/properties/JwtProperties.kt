package br.dev.demoraes.abrolhos.infrastructure.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application.security.jwt")
data class JwtProperties(
    val secretKey: String,
    val expiration: Long,
    val issuer: String
)
