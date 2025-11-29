package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "tags")
open class TagEntity(
    @Column(unique = true, nullable = false, length = 100)
    open var name: String,
    @Column(unique = true, nullable = false, length = 100)
    open var slug: String,
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    open var posts: MutableSet<PostEntity> = mutableSetOf(),
) : BaseEntity()
