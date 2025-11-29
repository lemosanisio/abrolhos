package br.dev.demoraes.abrolhos.infrastructure.configuration

import br.dev.demoraes.abrolhos.domain.entities.*
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.domain.services.CategoryService
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.TagService
import br.dev.demoraes.abrolhos.domain.services.commands.CreateCategoryCommand
import br.dev.demoraes.abrolhos.domain.services.commands.CreateTagCommand
import java.time.OffsetDateTime
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import ulid.ULID

@Configuration
class DataSeeder(
        private val userRepository: UserRepository,
        private val categoryService: CategoryService,
        private val tagService: TagService,
        private val postService: PostService,
        private val passwordEncoder: PasswordEncoder
) {

    @Bean
    fun seedData(): CommandLineRunner {
        return CommandLineRunner {
            if (userRepository.findByUsername(Username("admin")) == null) {
                val admin =
                        User(
                                id = ULID.nextULID(),
                                username = Username("admin"),
                                email = Email("admin@abrolhos.dev"),
                                passwordHash = PasswordHash(passwordEncoder.encode("admin123")),
                                role = Role.ADMIN,
                                createdAt = OffsetDateTime.now(),
                                updatedAt = OffsetDateTime.now()
                        )
                userRepository.save(admin)
                println("Seeded admin user")
            }

            if (categoryService.listCategories().isEmpty()) {
                categoryService.create(CreateCategoryCommand("Technology"))
                categoryService.create(CreateCategoryCommand("Travel"))
                categoryService.create(CreateCategoryCommand("Lifestyle"))
                println("Seeded categories")
            }

            if (tagService.listTags().isEmpty()) {
                tagService.create(CreateTagCommand("Kotlin"))
                tagService.create(CreateTagCommand("Spring Boot"))
                tagService.create(CreateTagCommand("React"))
                tagService.create(CreateTagCommand("HTMX"))
                println("Seeded tags")
            }

            println("=".repeat(60))
            println("Data seeding complete!")
            println("Admin credentials: admin / admin123")
            println("=".repeat(60))
        }
    }
}
