package com.lingo.application.user.usecase

import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.model.IssuedAccessJwt
import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.application.user.port.out.UserRepositoryPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.RawPassword
import com.lingo.domain.user.User

class FakeUserStore : UserRepositoryPort {
	val users = mutableListOf<User>()
	var existsCalls = 0
	var findCalls = 0
	var findFailure: RuntimeException? = null
	var saveCalls = 0
	var saveFailure: RuntimeException? = null
	private var sequence = 0L

	override fun save(user: User): User {
		saveCalls++
		saveFailure?.let { throw it }
		val saved = user.copy(id = ++sequence)
		users += saved
		return saved
	}

	override fun existsByEmail(email: Email): Boolean {
		existsCalls++
		return users.any { it.email == email }
	}

	override fun findByEmail(email: Email): User? {
		findCalls++
		findFailure?.let { throw it }
		return users.firstOrNull { it.email == email }
	}
}

class RecordingPasswordEncoder : PasswordEncoderPort {
	val received = mutableListOf<String>()
	val matchedHashes = mutableListOf<String>()

	override fun encode(raw: RawPassword): String {
		received += raw.value
		return "encoded:${raw.value}".reversed()
	}

	override fun matches(raw: String, hash: String): Boolean {
		matchedHashes += hash
		return "encoded:$raw".reversed() == hash
	}
}

class FakeTokenProvider : JwtProviderPort {
	val issuedFor = mutableListOf<User>()

	override fun issue(user: User): IssuedAccessJwt {
		issuedFor += user
		return IssuedAccessJwt("token-for-${user.id}", 3600)
	}

	override fun verify(token: String): AuthenticatedUser? = null
}
