package com.lingo.domain.user

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RawPasswordTest {

	@Test
	fun `P1 정확히 8자이면 성공한다`() {
		RawPassword.of("Abcdef1!")
	}

	@Test
	fun `P2 정확히 64자이면 성공한다`() {
		RawPassword.of("Abcdef1!" + "a".repeat(56))
	}

	@Test
	fun `P3 7자와 65자는 예외`() {
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcde1!") }
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcdef1!" + "a".repeat(57)) }
	}

	@Test
	fun `P4 영문 숫자 특수문자 중 하나라도 없으면 예외`() {
		assertFailsWith<InvalidPasswordException> { RawPassword.of("12345678!") }
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcdefgh!") }
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcdefg1") }
	}

	@Test
	fun `P5 공백과 한글은 예외`() {
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcd 123!") }
		assertFailsWith<InvalidPasswordException> { RawPassword.of("Abcd한글123!") }
	}

	@Test
	fun `P6 toString에 원문이 포함되지 않는다`() {
		val raw = "Abcdef1!"

		assertFalse(RawPassword.of(raw).toString().contains(raw))
	}

	@Test
	fun `P7 빈 문자열은 예외`() {
		assertFailsWith<InvalidPasswordException> { RawPassword.of("") }
	}
}
