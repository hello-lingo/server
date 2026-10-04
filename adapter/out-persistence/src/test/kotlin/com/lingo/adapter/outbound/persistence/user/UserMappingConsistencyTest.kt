package com.lingo.adapter.outbound.persistence.user

import com.lingo.domain.user.User
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

class UserMappingConsistencyTest {

	// 영속성 계층에만 존재해도 되는 인프라 컬럼
	private val allowedEntityOnly = setOf("createdAt", "updatedAt", "version")

	@Test
	fun `도메인 User와 UserJpaEntity의 업무 필드 집합이 일치한다`() {
		val domainFieldNames = User::class.memberProperties.map { it.name }.toSet()
		val entityFieldNames = UserJpaEntity::class.memberProperties.map { it.name }.toSet()

		assertEquals(domainFieldNames, entityFieldNames - allowedEntityOnly)
	}
}
