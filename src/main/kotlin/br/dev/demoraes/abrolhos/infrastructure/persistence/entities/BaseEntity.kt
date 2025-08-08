package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.OffsetDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = CURRENT_TIMESTAMP WHERE id =?")
@SQLRestriction("deleted_at IS NULL")
abstract class BaseEntity : Serializable {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    lateinit var id: String
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
        protected set

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: OffsetDateTime? = null
        protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as BaseEntity

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName}(id=$id)"
    }

    companion object {
        private const val serialVersionUID = 1L // Update this version value if BaseEntity is changed
    }
}
