package com.lingo.adapter.outbound.persistence

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
class FlywayMigrationTest @Autowired constructor(
	private val flyway: Flyway,
	private val jdbcTemplate: JdbcTemplate,
) {

	@Test
	fun `FW1 V1 마이그레이션이 성공으로 적용된다`() {
		val v1 = flyway.info().applied().firstOrNull { it.version?.version == "1" }

		assertNotNull(v1)
		assertEquals(MigrationState.SUCCESS, v1.state)
		assertEquals("create users", v1.description)
	}

	@Test
	fun `FW2 users 테이블에 이메일 고유 제약 uk_users_email이 있다`() {
		// H2는 따옴표 없는 식별자를 대문자로 저장한다 (users → USERS, uk_users_email → UK_USERS_EMAIL)
		val count = jdbcTemplate.queryForObject(
			"""
			select count(*) from information_schema.table_constraints
			where table_name = 'USERS' and constraint_name = 'UK_USERS_EMAIL' and constraint_type = 'UNIQUE'
			""".trimIndent(),
			Int::class.java,
		)

		assertEquals(1, count)
	}
}
