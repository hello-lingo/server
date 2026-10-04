package com.lingo.adapter.outbound.external.security

import com.lingo.domain.user.RawPassword
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BCryptPasswordEncoderAdapterTest {

	private val adapter = BCryptPasswordEncoderAdapter()
	private val raw = RawPassword.of("Abcdef1!")

	@Test
	fun `B1 결과는 BCrypt 형식이고 원문과 다르다`() {
		val hash = adapter.encode(raw)

		assertTrue(hash.startsWith("\$2"))
		assertNotEquals(raw.value, hash)
	}

	@Test
	fun `B2 같은 입력도 매번 다른 해시가 나온다`() {
		assertNotEquals(adapter.encode(raw), adapter.encode(raw))
	}

	@Test
	fun `B3 해시는 원문과 matches로 검증된다`() {
		val hash = adapter.encode(raw)

		assertTrue(BCryptPasswordEncoder().matches(raw.value, hash))
	}
}
