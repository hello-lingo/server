package com.lingo.adapter.outbound.persistence.user

import com.lingo.application.user.exception.DuplicateEmailException
import com.lingo.application.user.port.out.UserRepositoryPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
	private val repository: UserJpaRepository,
) : UserRepositoryPort {

	override fun save(user: User): User =
		try {
			UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)))
		} catch (e: DataIntegrityViolationException) {
			throw DuplicateEmailException(cause = e)
		}

	override fun existsByEmail(email: Email): Boolean = repository.existsByEmail(email.value)

	override fun findByEmail(email: Email): User? = repository.findByEmail(email.value)?.let(UserMapper::toDomain)
}
