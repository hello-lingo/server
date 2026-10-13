package com.lingo

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class FlywayIntegrationTest @Autowired constructor(
	private val flyway: Flyway,
	private val environment: Environment,
) {

	@Test
	fun `FW3 앱이 validate로 기동하고 V1 마이그레이션이 적용되어 있다`() {
		assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"))

		val v1 = flyway.info().applied().firstOrNull { it.version?.version == "1" }

		assertNotNull(v1)
		assertEquals(MigrationState.SUCCESS, v1.state)
	}
}
