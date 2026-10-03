package com.lingo.domain.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmailTest {

	@Test
	fun `E1 앞뒤 공백을 제거하고 소문자로 정규화한다`() {
		val email = Email.of("User@Example.com ")

		assertEquals("user@example.com", email.value)
	}

	@Test
	fun `E2 정상 형식은 생성된다`() {
		assertEquals("a@b.co", Email.of("a@b.co").value)
	}

	@Test
	fun `E3 형식 위반은 InvalidEmailException`() {
		val invalid = listOf(
			"",
			"abc.com",
			"@b.com",
			"a@",
			"a@b",
			"a b@c.com",
			"a".repeat(250) + "@b.com",
		)

		invalid.forEach { raw ->
			assertFailsWith<InvalidEmailException>("입력: $raw") { Email.of(raw) }
		}
	}

	@Test
	fun `E4 대소문자만 다른 입력은 동등하다`() {
		assertEquals(Email.of("A@B.com"), Email.of("a@b.COM"))
	}
}
