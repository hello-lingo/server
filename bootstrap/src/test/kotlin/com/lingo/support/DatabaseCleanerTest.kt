package com.lingo.support

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class DatabaseCleanerTest @Autowired constructor(
	private val databaseCleaner: DatabaseCleaner,
	private val jdbcTemplate: JdbcTemplate,
) {

	private fun count(table: String): Int =
		jdbcTemplate.queryForObject("select count(*) from $table", Int::class.java)!!

	@Test
	fun `DC1 users에 행이 있어도 clean 후에는 0행이다`() {
		jdbcTemplate.update(
			"insert into users (email, password_hash, name) values (?, ?, ?)",
			"cleaner@b.com",
			"hash",
			"홍길동",
		)
		// 다른 통합 테스트가 같은 메모리 DB에 행을 남겼을 수 있으므로 정확한 수 대신 "행이 있음"만 확인한다
		assertTrue(count("users") > 0)

		databaseCleaner.clean()

		assertEquals(0, count("users"))
	}

	@Test
	fun `DC2 clean은 Flyway 기록 테이블을 지우지 않는다`() {
		val before = count("\"flyway_schema_history\"")
		assertTrue(before > 0)

		databaseCleaner.clean()

		assertEquals(before, count("\"flyway_schema_history\""))
	}
}
