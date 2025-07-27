package br.dev.demoraes.abrolhos

import br.dev.demoraes.abrolhos.infrastructure.configuration.properties.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class AbrolhosApplication

fun main(args: Array<String>) {
	runApplication<AbrolhosApplication>(*args)
}
