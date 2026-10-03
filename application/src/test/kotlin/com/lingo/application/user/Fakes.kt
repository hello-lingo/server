package com.lingo.application.user

import com.lingo.application.user.port.out.ExistsUserByEmailPort
import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.application.user.port.out.SaveUserPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.RawPassword
import com.lingo.domain.user.User

class FakeUserStore : SaveUserPort, ExistsUserByEmailPort {
	val users = mutableListOf<User>()
	var existsCalls = 0
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
}

class RecordingPasswordEncoder : PasswordEncoderPort {
	val received = mutableListOf<String>()

	override fun encode(raw: RawPassword): String {
		received += raw.value
		return "encoded:${raw.value}".reversed()
	}
}
