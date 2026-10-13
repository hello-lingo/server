package com.lingo.domain.user

import com.lingo.domain.common.DomainException
import kotlin.test.Test
import kotlin.test.assertIs

class UserDomainExceptionTest {

	private val exceptions: List<Throwable> = listOf(
		InvalidEmailException("이메일"),
		InvalidPasswordException("비밀번호"),
		InvalidNameException("이름"),
	)

	@Test
	fun `DE1 세 예외는 모두 UserDomainException이다`() {
		exceptions.forEach { e ->
			assertIs<UserDomainException>(e, "예외: ${e::class.simpleName}")
		}
	}

	@Test
	fun `DE2 세 예외는 모두 공용 DomainException이다`() {
		exceptions.forEach { e ->
			assertIs<DomainException>(e, "예외: ${e::class.simpleName}")
		}
	}
}
