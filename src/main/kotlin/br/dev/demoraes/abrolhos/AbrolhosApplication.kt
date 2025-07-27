package br.dev.demoraes.abrolhos

import br.dev.demoraes.abrolhos.infrastructure.configuration.properties.JwtProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class AbrolhosApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AbrolhosApplication::class.java), args)
}
