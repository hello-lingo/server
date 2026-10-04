package com.lingo.application.user

import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.application.user.port.out.TokenProviderPort
import com.lingo.application.user.port.out.UserRepositoryPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.InvalidEmailException
import com.lingo.domain.user.RawPassword
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LoginService(
	private val userRepositoryPort: UserRepositoryPort,
	private val passwordEncoderPort: PasswordEncoderPort,
	private val tokenProviderPort: TokenProviderPort,
) {

	// 사용자가 없을 때도 해시 비교 비용을 치르도록 인코더가 만든 더미 해시를 쓴다
	private val dummyHash: String by lazy { passwordEncoderPort.encode(RawPassword.of(DUMMY_PASSWORD)) }

	fun login(command: LoginCommand): LoginResult {
		val user = findUser(command.email)
		val passwordMatches = passwordEncoderPort.matches(command.password, user?.passwordHash ?: dummyHash)
		if (user == null || !passwordMatches) {
			throw InvalidCredentialsException()
		}
		val issued = tokenProviderPort.issue(user)
		return LoginResult(accessToken = issued.token, expiresIn = issued.expiresIn)
	}

	private fun findUser(rawEmail: String) =
		try {
			userRepositoryPort.findByEmail(Email.of(rawEmail))
		} catch (e: InvalidEmailException) {
			null
		}

	private companion object {
		const val DUMMY_PASSWORD = "Dummy-Pass-1!"
	}
}
