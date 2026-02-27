package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ulid.ULID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
class PostMetricsPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var postService: PostService

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var meterRegistry: io.micrometer.core.instrument.MeterRegistry

    @Test
    fun `property - creating post records metric`() {
        val user =
                User(
                        id = ULID.nextULID(),
                        username = Username("author_${System.currentTimeMillis()}"),
                        totpSecret = null,
                        passwordHash = null,
                        isActive = true,
                        role = Role.USER,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now()
                )
        userRepository.save(user)

        val startCount = meterRegistry.counter("posts.created")?.count() ?: 0.0

        postService.createPost(
                title = "Test Post Metrics ${System.currentTimeMillis()}",
                content = "Content",
                status = PostStatus.DRAFT,
                categoryName = "Test",
                tagNames = listOf("tag1"),
                authorUsername = user.username.value
        )

        val endCount = meterRegistry.counter("posts.created")?.count() ?: 0.0
        require(endCount > startCount) { "Metric posts.created was not incremented" }
    }
}
