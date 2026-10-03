package com.lingo.adapter.outbound.persistence

import com.lingo.application.user.DuplicateEmailException
import com.lingo.application.user.port.out.ExistsUserByEmailPort
import com.lingo.application.user.port.out.SaveUserPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
	private val repository: UserJpaRepository,
) : SaveUserPort, ExistsUserByEmailPort {

	override fun save(user: User): User =
		try {
			UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)))
		} catch (e: DataIntegrityViolationException) {
			throw DuplicateEmailException(cause = e)
		}

	override fun existsByEmail(email: Email): Boolean = repository.existsByEmail(email.value)
}
