package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import ulid.ULID

interface CategoryRepository {
    fun findByName(name: CategoryName): Category?

    fun findByNameIn(names: Set<CategoryName>): Set<Category?>

    fun findBySlug(slug: CategorySlug): Category?

    fun findAll(): List<Category>

    fun findById(id: ULID): Category?

    fun save(category: Category): Category

    fun delete(category: Category)
}
