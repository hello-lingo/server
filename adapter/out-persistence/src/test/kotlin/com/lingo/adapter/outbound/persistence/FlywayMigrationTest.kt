package com.lingo.adapter.outbound.persistence

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

	@Test
	fun `FW3 V3 마이그레이션이 성공으로 적용된다`() {
		val v3 = flyway.info().applied().firstOrNull { it.version?.version == "3" }

		assertNotNull(v3)
		assertEquals(MigrationState.SUCCESS, v3.state)
		assertEquals("create schools", v3.description)
	}

	@Test
	fun `FW4 학교 이름은 전체에서 도메인은 학교 안에서 고유하다`() {
		val constraints = jdbcTemplate.queryForList(
			"""
			select table_name || '.' || constraint_name from information_schema.table_constraints
			where table_name in ('SCHOOLS', 'SCHOOL_DOMAINS') and constraint_type = 'UNIQUE'
			""".trimIndent(),
			String::class.java,
		).toSet()

		assertEquals(setOf("SCHOOLS.UK_SCHOOLS_NAME", "SCHOOL_DOMAINS.UK_SCHOOL_DOMAINS_SCHOOL_DOMAIN"), constraints)
	}

	@Test
	fun `FW5 V4 시드로 학교와 도메인이 들어 있다 (AC-3_1)`() {
		val v4 = flyway.info().applied().firstOrNull { it.version?.version == "4" }
		assertNotNull(v4)
		assertEquals(MigrationState.SUCCESS, v4.state)

		val schools = jdbcTemplate.queryForObject("select count(*) from schools", Int::class.java)!!
		assertTrue(schools >= 200, "학교 수: $schools")
		assertEquals(
			listOf("cau.ac.kr"),
			jdbcTemplate.queryForList(
				"select d.domain from school_domains d join schools s on s.id = d.school_id where s.name = '중앙대학교(다빈치캠퍼스)'",
				String::class.java,
			),
		)
	}

	@Test
	fun `FW6 시드의 모든 학교는 숨기지 않았고 도메인이 1개 이상이며 도메인은 정규화된 형식이다`() {
		val withoutDomain = jdbcTemplate.queryForObject(
			"select count(*) from schools s where not exists (select 1 from school_domains d where d.school_id = s.id)",
			Int::class.java,
		)
		assertEquals(0, withoutDomain)
		assertEquals(0, jdbcTemplate.queryForObject("select count(*) from schools where hidden", Int::class.java))

		val format = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")
		val invalid = jdbcTemplate.queryForList("select domain from school_domains", String::class.java).filterNot { format.matches(it.orEmpty()) }
		assertEquals(emptyList(), invalid)

		val untrimmed = jdbcTemplate.queryForList("select name from schools where name <> trim(name) or name = ''", String::class.java)
		assertEquals(emptyList(), untrimmed)
	}
}
