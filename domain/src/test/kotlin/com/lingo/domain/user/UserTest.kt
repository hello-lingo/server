package com.lingo.domain.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UserTest {

	private val email = Email.of("a@b.com")

	@Test
	fun `U1 signUp은 id 없이 필드를 보존한다`() {
		val user = User.signUp(email, "hash", "홍길동")

		assertNull(user.id)
		assertEquals(email, user.email)
		assertEquals("hash", user.passwordHash)
		assertEquals("홍길동", user.name)
	}

	@Test
	fun `U2 빈 이름 공백 이름 51자 이름은 예외`() {
		assertFailsWith<InvalidNameException> { User.signUp(email, "hash", "") }
		assertFailsWith<InvalidNameException> { User.signUp(email, "hash", "   ") }
		assertFailsWith<InvalidNameException> { User.signUp(email, "hash", "a".repeat(51)) }
	}

	@Test
	fun `U2 경계 50자는 성공한다`() {
		User.signUp(email, "hash", "a".repeat(50))
	}

	@Test
	fun `U3 이름 앞뒤 공백은 제거된다`() {
		assertEquals("홍길동", User.signUp(email, "hash", "  홍길동 ").name)
	}

	@Test
	fun `U4 copy는 원본을 바꾸지 않는다`() {
		val original = User.signUp(email, "hash", "홍길동")

		val copied = original.copy(id = 1)

		assertEquals(1L, copied.id)
		assertNull(original.id)
	}
}
