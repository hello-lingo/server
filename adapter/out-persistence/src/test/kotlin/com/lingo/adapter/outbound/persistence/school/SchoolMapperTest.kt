package com.lingo.adapter.outbound.persistence.school

import com.lingo.domain.school.RegisteredDomain
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class SchoolMapperTest {

	@Test
	fun `SM1 id가 있는 School은 왕복 후에도 동등하다`() {
		val school = School(
			id = 1L,
			name = SchoolName.of("중앙대학교"),
			domains = listOf(RegisteredDomain(10L, SchoolDomain.of("cau.ac.kr")), RegisteredDomain(11L, SchoolDomain.of("old.cau.ac.kr"))),
			hidden = true,
		)

		assertEquals(school, SchoolMapper.toDomain(SchoolMapper.toEntity(school)))
	}

	@Test
	fun `SM2 새 학교는 학교와 도메인 id가 null이고 도메인은 학교를 가리킨다`() {
		val entity = SchoolMapper.toEntity(School.create(SchoolName.of("중앙대학교"), listOf(SchoolDomain.of("cau.ac.kr"))))

		assertNull(entity.id)
		assertNull(entity.domains.single().id)
		assertSame(entity, entity.domains.single().school)
	}
}
