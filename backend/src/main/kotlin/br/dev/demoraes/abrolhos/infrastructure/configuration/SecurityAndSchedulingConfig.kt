package br.dev.demoraes.abrolhos.infrastructure.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableScheduling
@EnableMethodSecurity
class SecurityAndSchedulingConfig
