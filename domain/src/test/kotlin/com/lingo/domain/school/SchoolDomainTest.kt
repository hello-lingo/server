package com.lingo.domain.school

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchoolDomainTest {

	@Test
	fun `SD1 앞뒤 공백을 제거하고 소문자로 정규화한다`() {
		assertEquals("cau.ac.kr", SchoolDomain.of(" CAU.ac.KR ").value)
	}

	@Test
	fun `SD2 정상 형식은 생성된다`() {
		listOf("cau.ac.kr", "snu.ac.kr", "my-univ.ac.kr", "a.kr", "mail.korea.ac.kr", "u1.edu").forEach { raw ->
			assertEquals(raw, SchoolDomain.of(raw).value, "입력: $raw")
		}
	}

	@Test
	fun `SD3 형식 위반은 InvalidSchoolDomainException`() {
		val invalid = listOf(
			"",
			"   ",
			"cau",
			"@cau.ac.kr",
			"user@cau.ac.kr",
			"cau ac.kr",
			".cau.ac.kr",
			"cau.ac.kr.",
			"cau..ac.kr",
			"-cau.ac.kr",
			"cau-.ac.kr",
			"cau_univ.ac.kr",
			"https://cau.ac.kr",
			"a".repeat(251) + ".kr", // 254자
		)

		invalid.forEach { raw ->
			assertFailsWith<InvalidSchoolDomainException>("입력: $raw") { SchoolDomain.of(raw) }
		}
	}

	@Test
	fun `SD4 253자까지 허용한다`() {
		val label = "a".repeat(61)
		val raw = listOf(label, label, label, label).joinToString(".") + ".kr" // 61*4 + 3 + 3 = 250
		val padded = "abc" + raw // 253
		assertEquals(253, padded.length)
		assertEquals(padded, SchoolDomain.of(padded).value)
		assertFailsWith<InvalidSchoolDomainException> { SchoolDomain.of("a$padded") }
	}

	@Test
	fun `SD5 대소문자만 다른 입력은 동등하다`() {
		assertEquals(SchoolDomain.of("CAU.ac.kr"), SchoolDomain.of("cau.AC.KR"))
	}
}
