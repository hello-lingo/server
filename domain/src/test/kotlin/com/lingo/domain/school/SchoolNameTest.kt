package com.lingo.domain.school

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchoolNameTest {

	@Test
	fun `SN1 앞뒤 공백을 제거한다`() {
		assertEquals("중앙대학교", SchoolName.of("  중앙대학교 ").value)
	}

	@Test
	fun `SN2 1자부터 100자까지 허용한다`() {
		assertEquals("가", SchoolName.of("가").value)
		assertEquals(100, SchoolName.of("가".repeat(100)).value.length)
	}

	@Test
	fun `SN3 비어 있거나 100자를 넘으면 InvalidSchoolNameException`() {
		listOf("", "   ", "가".repeat(101)).forEach { raw ->
			assertFailsWith<InvalidSchoolNameException>("입력 길이: ${raw.length}") { SchoolName.of(raw) }
		}
	}
}
