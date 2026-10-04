package com.lingo.application.user

import com.lingo.domain.user.Email
import com.lingo.domain.user.InvalidEmailException
import com.lingo.domain.user.InvalidNameException
import com.lingo.domain.user.InvalidPasswordException
import com.lingo.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class SignUpServiceTest {

	private val store = FakeUserStore()
	private val encoder = RecordingPasswordEncoder()
	private val service = SignUpService(store, encoder)
	private val command = SignUpCommand("User@Example.com", "Abcdef1!", "홍길동")

	@Test
	fun `S1 성공하면 1건 저장하고 채번된 id와 정규화된 이메일을 반환한다`() {
		val result = service.signUp(command)

		assertEquals(1, store.users.size)
		assertEquals(SignUpResult(userId = 1L, email = "user@example.com"), result)
	}

	@Test
	fun `S2 저장된 해시는 원문과 다르고 encoder는 원문을 받는다`() {
		service.signUp(command)

		assertNotEquals("Abcdef1!", store.users.single().passwordHash)
		assertEquals(listOf("Abcdef1!"), encoder.received)
	}

	@Test
	fun `S3 대소문자만 다른 이메일이 있으면 DuplicateEmailException이고 저장과 해싱은 없다`() {
		store.users += User(10L, Email.of("user@example.com"), "hash", "기존")

		assertFailsWith<DuplicateEmailException> { service.signUp(command) }

		assertEquals(0, store.saveCalls)
		assertEquals(0, encoder.received.size)
	}

	@Test
	fun `S4 비밀번호 정책 위반이면 포트를 호출하지 않는다`() {
		assertFailsWith<InvalidPasswordException> { service.signUp(command.copy(password = "short")) }

		assertNoPortCalls()
	}

	@Test
	fun `S4 이메일 형식 위반이면 포트를 호출하지 않는다`() {
		assertFailsWith<InvalidEmailException> { service.signUp(command.copy(email = "nope")) }

		assertNoPortCalls()
	}

	@Test
	fun `S4 이름 위반이면 포트를 호출하지 않는다`() {
		assertFailsWith<InvalidNameException> { service.signUp(command.copy(name = "  ")) }

		assertNoPortCalls()
	}

	@Test
	fun `S5 저장 포트의 DuplicateEmailException은 그대로 전파된다`() {
		val failure = DuplicateEmailException("race")
		store.saveFailure = failure

		val thrown = assertFailsWith<DuplicateEmailException> { service.signUp(command) }

		assertSame(failure, thrown)
	}

	@Test
	fun `S6 결과는 userId와 email만 가진다`() {
		val result = SignUpResult(userId = 1L, email = "a@b.com")

		assertEquals("SignUpResult(userId=1, email=a@b.com)", result.toString())
	}

	@Test
	fun `command toString은 비밀번호 원문을 노출하지 않는다`() {
		assertEquals(false, command.toString().contains("Abcdef1!"))
	}

	private fun assertNoPortCalls() {
		assertEquals(0, store.existsCalls)
		assertEquals(0, store.saveCalls)
		assertEquals(0, encoder.received.size)
	}
}
