package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.services.commands.CreateCategoryCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateCategoryCommand
import java.time.OffsetDateTime
import org.springframework.stereotype.Service
import ulid.ULID

@Service
class CategoryService(
        private val categoryRepository: CategoryRepository,
) {
    fun listCategories(): List<Category> = categoryRepository.findAll()

    fun getBySlug(slug: String): Category? = categoryRepository.findBySlug(CategorySlug(slug))

    fun create(command: CreateCategoryCommand): Category {
        val name = CategoryName(command.name)
        val slug = generateSlug(command.name)

        // Check if category with same name or slug already exists
        check(categoryRepository.findByName(name) == null) {
            "Category with name ${command.name} already exists"
        }
        check(categoryRepository.findBySlug(slug) == null) {
            "Category with slug ${slug.value} already exists"
        }

        val now = OffsetDateTime.now()
        val category =
                Category(
                        id = ULID.nextULID(),
                        name = name,
                        slug = slug,
                        posts = emptySet(),
                        createdAt = now,
                        updatedAt = now,
                )

        return categoryRepository.save(category)
    }

    fun update(command: UpdateCategoryCommand): Category {
        val name = CategoryName(command.name)
        val slug = generateSlug(command.name)

        val category =
                categoryRepository.findById(ULID.parseULID(command.id))
                        ?: throw NoSuchElementException("Category with ID ${command.id} not found")

        // Check if updated name/slug would conflict with existing categories
        categoryRepository.findByName(name)?.let {
            check(it.id == category.id) { "Category with name ${command.name} already exists" }
        }
        categoryRepository.findBySlug(slug)?.let {
            check(it.id == category.id) { "Category with slug ${slug.value} already exists" }
        }

        val updatedCategory =
                category.copy(
                        name = name,
                        slug = slug,
                        updatedAt = OffsetDateTime.now(),
                )

        return categoryRepository.save(updatedCategory)
    }

    fun delete(id: String) {
        val category =
                categoryRepository.findById(ULID.parseULID(id))
                        ?: throw NoSuchElementException("Category with ID $id not found")

        categoryRepository.delete(category)
    }

    private fun generateSlug(name: String): CategorySlug {
        val slug = name.lowercase().replace(Regex("[^a-z0-9\\s-]"), "").replace(Regex("\\s+"), "-")
        return CategorySlug(slug)
    }
}
