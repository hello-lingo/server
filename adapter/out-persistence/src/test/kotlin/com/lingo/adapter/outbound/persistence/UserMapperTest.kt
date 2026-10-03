package com.lingo.adapter.outbound.persistence

import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserMapperTest {

	@Test
	fun `M1 id가 있는 User는 왕복 후에도 동등하다`() {
		val user = User(7L, Email.of("a@b.com"), "hash", "홍길동")

		assertEquals(user, UserMapper.toDomain(UserMapper.toEntity(user)))
	}

	@Test
	fun `M1 id가 null이면 엔티티 id도 null이고 왕복 후 동등하다`() {
		val user = User.signUp(Email.of("a@b.com"), "hash", "홍길동")

		val entity = UserMapper.toEntity(user)

		assertNull(entity.id)
		assertEquals(user, UserMapper.toDomain(entity))
	}
}
