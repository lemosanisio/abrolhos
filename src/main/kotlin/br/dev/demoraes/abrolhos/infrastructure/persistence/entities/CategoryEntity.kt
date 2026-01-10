package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
open class CategoryEntity(
    @Column(unique = true, nullable = false, length = 100)
    open var name: String,
    @Column(unique = true, nullable = false, length = 100)
    open var slug: String,
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    open var posts: MutableSet<PostEntity> = mutableSetOf(),
) : BaseEntity()
