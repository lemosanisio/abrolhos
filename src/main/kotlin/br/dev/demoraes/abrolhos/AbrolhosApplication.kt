package br.dev.demoraes.abrolhos

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class AbrolhosApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AbrolhosApplication::class.java), args)
}
