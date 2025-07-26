package br.dev.demoraes.abrolhos.infraestructure.postgresql.repositories

import br.dev.demoraes.abrolhos.domain.authentication.entities.Email
import br.dev.demoraes.abrolhos.domain.authentication.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.authentication.entities.User
import br.dev.demoraes.abrolhos.domain.authentication.entities.Username
import br.dev.demoraes.abrolhos.domain.authentication.repository.UserRepository
import br.dev.demoraes.abrolhos.infraestructure.postgresql.entities.UserEntity
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class UserRepositoryImpl(
    val userRepositoryPostgresql: UserRepositoryPostgresql
): UserRepository {
    override fun save(user: User): User {
        return userRepositoryPostgresql.save<UserEntity>(user.toEntity()).toDomain()
    }

    override fun findById(id: ULID): User? {
        return userRepositoryPostgresql.findByIdOrNull(id)?.toDomain()
    }

    override fun findByUsername(username: Username): User? {
        return userRepositoryPostgresql.findByUsername(username.toString())?.toDomain()
    }
}

private fun User.toEntity() = UserEntity(
    id = this.id,
    username = this.username.value,
    email = this.email.value,
    passwordHash = this.passwordHash.value,
    role = this.role
)

private fun UserEntity.toDomain() = User(
    id = this.id,
    username = Username(this.username),
    email = Email(this.email),
    passwordHash = PasswordHash(this.passwordHash),
    role = this.role
)