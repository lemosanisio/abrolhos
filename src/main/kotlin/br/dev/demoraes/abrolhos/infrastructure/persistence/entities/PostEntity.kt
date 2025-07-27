package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import br.dev.demoraes.abrolhos.domain.authentication.entities.Category
import br.dev.demoraes.abrolhos.domain.authentication.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.authentication.entities.Tag
import br.dev.demoraes.abrolhos.domain.authentication.entities.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "posts")
open class PostEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    open var author: User,

    @Column(nullable = false, length = 255)
    open var title: String,

    @Column(unique = true, nullable = false, length = 255)
    open var slug: String,

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    open var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    open var status: PostStatus = PostStatus.DRAFT,

    @Column(name = "published_at")
    open var publishedAt: OffsetDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    open var category: Category? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_tags",
        joinColumns = [JoinColumn(name = "post_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    open var tags: MutableSet<Tag> = mutableSetOf()
) : BaseEntity()
