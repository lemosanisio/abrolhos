package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName

interface CategoryRepository {
    fun findByName(name: CategoryName): Category?
    fun findByNameIn(names: Set<CategoryName>): Set<Category?>
}
