package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.authentication.entities.Email
import br.dev.demoraes.abrolhos.domain.authentication.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.authentication.entities.User
import br.dev.demoraes.abrolhos.domain.authentication.entities.Username
import br.dev.demoraes.abrolhos.domain.authentication.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.UserRepositoryPostgresql
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class UserRepositoryImpl(
    private val userRepositoryPostgresql: UserRepositoryPostgresql
): UserRepository {
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

private fun User.toEntity() = UserEntity(
    id = this.id.toString(),
    username = this.username.value,
    email = this.email.value,
    passwordHash = this.passwordHash.value,
    role = this.role
)

private fun UserEntity.toDomain() = User(
    id = ULID.parseULID(this.id),
    username = Username(this.username),
    email = Email(this.email),
    passwordHash = PasswordHash(this.passwordHash),
    role = this.role
)