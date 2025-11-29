package br.dev.demoraes.abrolhos.infrastructure.config

import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.domain.services.PostService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfig {

    @Bean
    fun postService(
        postRepository: PostRepository,
        userRepository: UserRepository,
        categoryRepository: CategoryRepository,
        tagRepository: TagRepository
    ): PostService {
        return PostService(
            postRepository,
            userRepository,
            categoryRepository,
            tagRepository
        )
    }
}
