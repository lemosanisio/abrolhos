package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Email
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.UserRepositoryPostgresql
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class UserRepositoryImpl(
    private val userRepositoryPostgresql: UserRepositoryPostgresql,
) : UserRepository {
    override fun save(user: User): User {
        return userRepositoryPostgresql.save<UserEntity>(user.toEntity()).toDomain()
    }

    override fun findById(id: ULID): User? {
        return userRepositoryPostgresql.findByIdOrNull(id.toString())?.toDomain()
    }

    override fun findByUsername(username: Username): User? {
        return userRepositoryPostgresql.findByUsername(username.value)?.toDomain()
    }
}

fun User.toEntity() =
    UserEntity(
        username = this.username.value,
        email = this.email.value,
        passwordHash = this.passwordHash.value,
        role = this.role,
    )
        .apply {
            id = this@toEntity.id.toString()
            createdAt = this@toEntity.createdAt
            updatedAt = this@toEntity.updatedAt
        }

fun UserEntity.toDomain(): User {
    val createdAt =
        this.createdAt
            ?: throw IllegalStateException(
                "PostEntity with id ${this.id} is missing a createdAt timestamp. " +
                    "This should not happen for a persisted entity.",
            )

    val updatedAt =
        this.updatedAt
            ?: throw IllegalStateException(
                "PostEntity with id ${this.id} is missing an updatedAt timestamp. " +
                    "This should not happen for a persisted entity.",
            )

    return User(
        id = ULID.parseULID(this.id),
        username = Username(this.username),
        email = Email(this.email),
        passwordHash = PasswordHash(this.passwordHash),
        role = this.role,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
