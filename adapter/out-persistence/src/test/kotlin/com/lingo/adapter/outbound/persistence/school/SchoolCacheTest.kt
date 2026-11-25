package com.lingo.adapter.outbound.persistence.school

import com.github.benmanes.caffeine.cache.Cache as CaffeineCache
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.cache.transaction.TransactionAwareCacheDecorator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 캐시 동작은 커밋 시점에 달려 있어서 @DataJpaTest(테스트마다 롤백) 대신 실제로 커밋되는 컨텍스트로 확인한다.
 * 캐시를 거치지 않는 JdbcTemplate으로 행을 직접 넣어, 목록이 캐시에서 나왔는지 DB에서 나왔는지 구분한다.
 */
@SpringBootTest
class SchoolCacheTest @Autowired constructor(
	private val adapter: SchoolPersistenceAdapter,
	private val cacheManager: CacheManager,
	private val jdbcTemplate: JdbcTemplate,
	private val transactionTemplate: TransactionTemplate,
) {

	@BeforeEach
	fun reset() {
		jdbcTemplate.update("delete from school_domains")
		jdbcTemplate.update("delete from schools")
		cacheManager.getCache(SchoolCacheConfig.SCHOOLS)!!.clear()
	}

	private fun newSchool(name: String, domain: String) = School.create(SchoolName.of(name), listOf(SchoolDomain.of(domain)))

	/** 캐시를 거치지 않고 DB에 학교를 넣는다 */
	private fun insertDirectly(name: String, domain: String) {
		jdbcTemplate.update("insert into schools (name, hidden) values (?, false)", name)
		val id = jdbcTemplate.queryForObject("select id from schools where name = ?", Long::class.java, name)
		jdbcTemplate.update("insert into school_domains (school_id, domain) values (?, ?)", id, domain)
	}

	private fun names() = adapter.findAll().map { it.name.value }

	@Test
	fun `CA1 findAll은 두 번째부터 캐시에서 준다`() {
		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))
		assertEquals(listOf("중앙대학교"), names())

		insertDirectly("서울대학교", "snu.ac.kr")

		assertEquals(listOf("중앙대학교"), names())
	}

	@Test
	fun `CA2 save가 커밋되면 캐시를 비워 다음 findAll이 DB를 다시 읽는다`() {
		assertEquals(emptyList(), names())
		insertDirectly("서울대학교", "snu.ac.kr")

		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))

		assertEquals(setOf("서울대학교", "중앙대학교"), names().toSet())
	}

	@Test
	fun `CA3 deleteById가 커밋되면 캐시를 비운다`() {
		val saved = adapter.save(newSchool("중앙대학교", "cau.ac.kr"))
		assertEquals(listOf("중앙대학교"), names())

		adapter.deleteById(saved.id!!)

		assertEquals(emptyList(), names())
	}

	@Test
	fun `CA4 트랜잭션이 롤백되면 캐시를 비우지 않는다 (커밋 후에만 비움)`() {
		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))
		assertEquals(listOf("중앙대학교"), names())
		insertDirectly("서울대학교", "snu.ac.kr")

		transactionTemplate.executeWithoutResult { status ->
			adapter.save(newSchool("고려대학교", "korea.ac.kr"))
			status.setRollbackOnly()
		}

		// 비워졌다면 DB를 다시 읽어 서울대학교가 보였을 것이다
		assertEquals(listOf("중앙대학교"), names())
	}

	@Test
	fun `CA5 schools 캐시는 커밋 후 반영되고 10분 뒤 만료된다 (D9 안전장치)`() {
		val cache = assertIs<TransactionAwareCacheDecorator>(cacheManager.getCache(SchoolCacheConfig.SCHOOLS))
		@Suppress("UNCHECKED_CAST")
		val caffeine = cache.targetCache.nativeCache as CaffeineCache<Any, Any>

		assertEquals(Duration.ofMinutes(10), caffeine.policy().expireAfterWrite().get().expiresAfter)
	}
}
