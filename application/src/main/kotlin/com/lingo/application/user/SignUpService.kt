package com.lingo.application.user

import com.lingo.application.user.port.out.ExistsUserByEmailPort
import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.application.user.port.out.SaveUserPort
import com.lingo.domain.user.Email
import com.lingo.domain.user.RawPassword
import com.lingo.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignUpService(
	private val existsUserByEmailPort: ExistsUserByEmailPort,
	private val passwordEncoderPort: PasswordEncoderPort,
	private val saveUserPort: SaveUserPort,
) {

	fun signUp(command: SignUpCommand): SignUpResult {
		val email = Email.of(command.email)
		val password = RawPassword.of(command.password)
		val name = User.validateName(command.name)

		if (existsUserByEmailPort.existsByEmail(email)) {
			throw DuplicateEmailException()
		}

		val hash = passwordEncoderPort.encode(password)
		val saved = saveUserPort.save(User.signUp(email, hash, name))
		val userId = requireNotNull(saved.id) { "저장된 사용자에 id가 없습니다." }
		return SignUpResult(userId = userId, email = saved.email.value)
	}
}
