package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "posts")
class PostEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: UserEntity,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(unique = true, nullable = false, length = 255)
    var slug: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PostStatus = PostStatus.DRAFT,
    @Column(name = "published_at")
    var publishedAt: OffsetDateTime? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: CategoryEntity? = null,
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_tags",
        joinColumns = [JoinColumn(name = "post_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    var tags: MutableSet<TagEntity> = mutableSetOf(),
) : BaseEntity()
