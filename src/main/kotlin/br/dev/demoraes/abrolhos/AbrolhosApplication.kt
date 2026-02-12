package br.dev.demoraes.abrolhos

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class AbrolhosApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AbrolhosApplication::class.java), args)
}
