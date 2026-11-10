package com.lingo.adapter.outbound.persistence.school

import com.lingo.domain.school.RegisteredDomain
import com.lingo.domain.school.School
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

class SchoolMappingConsistencyTest {

	@Test
	fun `도메인 School과 SchoolJpaEntity의 업무 필드 집합이 일치한다`() {
		assertEquals(
			School::class.memberProperties.map { it.name }.toSet(),
			SchoolJpaEntity::class.memberProperties.map { it.name }.toSet(),
		)
	}

	@Test
	fun `도메인 RegisteredDomain과 SchoolDomainJpaEntity의 업무 필드 집합이 일치한다 (부모 참조 school 제외)`() {
		assertEquals(
			RegisteredDomain::class.memberProperties.map { it.name }.toSet(),
			SchoolDomainJpaEntity::class.memberProperties.map { it.name }.toSet() - "school",
		)
	}
}
