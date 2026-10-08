package com.lingo.application.user.port.out

import com.lingo.domain.user.Email
import com.lingo.domain.user.User

interface UserRepositoryPort {
	fun save(user: User): User

	fun existsByEmail(email: Email): Boolean

	fun findByEmail(email: Email): User?
}
