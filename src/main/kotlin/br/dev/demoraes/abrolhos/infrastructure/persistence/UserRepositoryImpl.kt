package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.UserRepositoryPostgresql
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class UserRepositoryImpl(
    private val userRepositoryPostgresql: UserRepositoryPostgresql,
) : UserRepository {
    private val logger = LoggerFactory.getLogger(UserRepositoryImpl::class.java)

    override fun findById(id: ULID): User? {
        logger.info("Searching user by id $id")
        return userRepositoryPostgresql.findById(id.toString())
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByUsername(username: Username): User? {
        logger.info("Searching user name $username")
        return userRepositoryPostgresql.findByUsername(username.value)?.toDomain()
    }

    override fun save(user: User): User {
        logger.info("Saving user ${user.username}")
        val entity = user.toEntity()
        return userRepositoryPostgresql.save(entity).toDomain()
    }

    override fun existsByUsername(username: Username): Boolean {
        logger.info("Checking if username exists: $username")
        return userRepositoryPostgresql.existsByUsername(username.value)
    }
}

internal fun User.toEntity() =
    UserEntity(
        username = this.username.value,
        totpSecret = this.totpSecret?.value,
        isActive = this.isActive,
        role = this.role,
    )
        .apply {
            id = this@toEntity.id.toString()
            createdAt = this@toEntity.createdAt
            updatedAt = this@toEntity.updatedAt
        }

internal fun UserEntity.toDomain(): User {
    val createdAt =
        this.createdAt
            ?: throw IllegalStateException(
                "UserEntity with id ${this.id} is missing a createdAt timestamp. " +
                    "This should not happen for a persisted entity.",
            )

    val updatedAt =
        this.updatedAt
            ?: throw IllegalStateException(
                "UserEntity with id ${this.id} is missing an updatedAt timestamp. " +
                    "This should not happen for a persisted entity.",
            )

    return User(
        id = ULID.parseULID(this.id),
        username = Username(this.username),
        totpSecret = this.totpSecret?.let { TotpSecret(it) },
        isActive = this.isActive,
        role = this.role,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
