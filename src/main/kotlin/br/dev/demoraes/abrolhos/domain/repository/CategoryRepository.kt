package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName

/**
 * Repository interface for Category entity persistence.
 *
 * Handles storage and retrieval of blog categories.
 */
interface CategoryRepository {
    fun findByName(name: CategoryName): Category?

    fun save(category: Category): Category
}
