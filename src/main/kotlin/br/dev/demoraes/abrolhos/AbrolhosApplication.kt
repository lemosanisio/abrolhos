package br.dev.demoraes.abrolhos

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration

/**
 * Main entry point for the Abrolhos application.
 *
 * This class triggers the Spring Boot auto-configuration and component scanning.
 * It excludes UserDetailsServiceAutoConfiguration because we use a custom authentication mechanism (JWT).
 *
 * Application Flow:
 * 1. Spring Boot starts and scans for components in the `br.dev.demoraes.abrolhos` package.
 * 2. It initializes the database connection, Redis connection, and web server (Tomcat/Jetty).
 * 3. Configuration classes (SecurityConfig, WebConfig, etc.) serve to set up the environment.
 * 4. Control is handed over to the DispatcherServlet to handle incoming HTTP requests.
 */
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class AbrolhosApplication

fun main(args: Array<String>) {
    SpringApplication.run(arrayOf(AbrolhosApplication::class.java), args)
}
