package com.lingo.adapter.outbound.persistence.user

import com.lingo.domain.user.Email
import com.lingo.domain.user.User

internal object UserMapper {

	fun toEntity(user: User): UserJpaEntity =
		UserJpaEntity(id = user.id, email = user.email.value, passwordHash = user.passwordHash, name = user.name)

	fun toDomain(entity: UserJpaEntity): User =
		User(id = entity.id, email = Email.of(entity.email), passwordHash = entity.passwordHash, name = entity.name)
}
