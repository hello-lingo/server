package com.lingo.support

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

// 테스트 DB가 H2이므로 H2 문법. 반환되는 이름: USERS(따옴표 없이 만든 테이블은 대문자), flyway_schema_history(따옴표로 만든 소문자)
@Component
class DatabaseCleaner(private val jdbcTemplate: JdbcTemplate) {
	fun clean() {
		val tables = jdbcTemplate.queryForList(
			"select table_name from information_schema.tables where table_schema = 'PUBLIC' and table_type = 'BASE TABLE'",
			String::class.java,
		).filterNot { it.equals("flyway_schema_history", ignoreCase = true) }

		// H2의 referential_integrity는 DB 전체 설정이고, 여러 테스트 컨텍스트가 같은 메모리 DB를 쓴다.
		// truncate가 실패해도 외래키 검사가 꺼진 채로 남지 않도록 finally에서 다시 켠다.
		jdbcTemplate.execute("set referential_integrity false")
		try {
			tables.forEach { jdbcTemplate.execute("truncate table \"$it\" restart identity") }
		} finally {
			jdbcTemplate.execute("set referential_integrity true")
		}
	}
}
