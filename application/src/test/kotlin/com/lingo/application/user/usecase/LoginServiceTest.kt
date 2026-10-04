package com.lingo.application.user.usecase

import com.lingo.application.user.IssuedAccessToken
import com.lingo.application.user.exception.InvalidCredentialsException
import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginServiceTest {

	private val store = FakeUserStore()
	private val encoder = RecordingPasswordEncoder()
	private val tokens = FakeTokenProvider()
	private val service = LoginService(store, encoder, tokens)

	@BeforeTest
	fun registerUser() {
		store.save(User.signUp(Email.of("a@b.com"), "encoded:Abcdef1!".reversed(), "홍길동"))
		encoder.received.clear()
	}

	@Test
	fun `A1 올바른 이메일과 비밀번호면 토큰을 발급한다`() {
		val result = service.login(LoginCommand("a@b.com", "Abcdef1!"))

		assertEquals(LoginResult("token-for-1", 3600), result)
		assertEquals(1, tokens.issuedFor.size)
	}

	@Test
	fun `A2 사용자가 없으면 InvalidCredentialsException이고 토큰을 발급하지 않는다`() {
		assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("x@b.com", "Abcdef1!")) }

		assertTrue(tokens.issuedFor.isEmpty())
	}

	@Test
	fun `A3 비밀번호가 틀리면 사용자 없음과 같은 예외와 메시지다`() {
		val wrong = assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("a@b.com", "Wrong1!aa")) }
		val missing = assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("x@b.com", "Abcdef1!")) }

		assertEquals(missing.message, wrong.message)
		assertTrue(tokens.issuedFor.isEmpty())
	}

	@Test
	fun `A4 이메일은 정규화되어 조회된다`() {
		val result = service.login(LoginCommand("  A@B.com ", "Abcdef1!"))

		assertEquals("token-for-1", result.accessToken)
	}

	@Test
	fun `A5 형식 불량 이메일은 InvalidCredentialsException이고 matches는 호출된다`() {
		assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("not-an-email", "Abcdef1!")) }

		assertEquals(1, encoder.matchedHashes.size)
	}

	@Test
	fun `A6 사용자가 없으면 더미 해시로 비교하고 더미 encode는 한 번만 한다`() {
		assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("x@b.com", "Abcdef1!")) }
		assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("y@b.com", "Abcdef1!")) }

		assertEquals(2, encoder.matchedHashes.size)
		assertEquals(1, encoder.received.size)
		assertEquals(encoder.matchedHashes[0], encoder.matchedHashes[1])
		assertFalse(encoder.matchedHashes.contains(store.users[0].passwordHash))
	}

	@Test
	fun `A7 정책 미달 비밀번호도 InvalidCredentialsException이다`() {
		assertFailsWith<InvalidCredentialsException> { service.login(LoginCommand("a@b.com", "x")) }
	}

	@Test
	fun `A8 toString은 비밀번호와 토큰 원문을 노출하지 않는다`() {
		assertFalse(LoginCommand("a@b.com", "Abcdef1!").toString().contains("Abcdef1!"))
		assertFalse(IssuedAccessToken("secret-token", 3600).toString().contains("secret-token"))
	}

	@Test
	fun `A9 저장소 예외는 그대로 전파된다`() {
		store.findFailure = IllegalStateException("db down")

		assertFailsWith<IllegalStateException> { service.login(LoginCommand("a@b.com", "Abcdef1!")) }
	}
}
