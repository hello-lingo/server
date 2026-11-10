package com.lingo.adapter.outbound.persistence.school

import com.lingo.application.school.exception.DuplicateSchoolNameException
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@Import(SchoolPersistenceAdapter::class)
class SchoolPersistenceAdapterTest @Autowired constructor(
	private val adapter: SchoolPersistenceAdapter,
	private val entityManager: EntityManager,
	private val jdbcTemplate: JdbcTemplate,
) {

	private fun domain(raw: String) = SchoolDomain.of(raw)

	private fun newSchool(name: String = "중앙대학교", vararg domains: String = arrayOf("cau.ac.kr")) =
		School.create(SchoolName.of(name), domains.map(::domain))

	/** 영속성 컨텍스트를 비워 DB에서 다시 읽는다 */
	private fun reload(id: Long): School {
		entityManager.flush()
		entityManager.clear()
		return assertNotNull(adapter.findById(id))
	}

	private fun domainRowCount(): Int = jdbcTemplate.queryForObject("select count(*) from school_domains", Int::class.java)!!

	@Test
	fun `SR1 새 학교를 저장하면 학교와 도메인에 id가 채워지고 재조회 값이 같다`() {
		val saved = adapter.save(newSchool("중앙대학교", "cau.ac.kr", "old.cau.ac.kr"))

		val id = assertNotNull(saved.id)
		assertTrue(saved.domains.all { it.id != null })
		assertEquals(saved, reload(id))
	}

	@Test
	fun `SR2 이름 변경 숨김 도메인 추가를 저장하면 반영되고 기존 도메인 id는 그대로다`() {
		val saved = adapter.save(newSchool())
		val cauId = saved.domains.single().id

		adapter.save(saved.rename(SchoolName.of("중앙대학교(서울캠퍼스)")).changeVisibility(hidden = true).addDomain(domain("old.cau.ac.kr")))
		val found = reload(saved.id!!)

		assertEquals("중앙대학교(서울캠퍼스)", found.name.value)
		assertTrue(found.hidden)
		assertEquals(listOf("cau.ac.kr", "old.cau.ac.kr"), found.domains.map { it.domain.value })
		assertEquals(cauId, found.domains.first().id)
	}

	@Test
	fun `SR3 도메인 값을 바꾸면 같은 id 행이 바뀐다`() {
		val saved = adapter.save(newSchool("중앙대학교", "cau.ac.kr", "old.cau.ac.kr"))
		val oldId = saved.domains[1].id!!

		adapter.save(saved.changeDomain(oldId, domain("new.cau.ac.kr")))
		val found = reload(saved.id!!)

		assertEquals(oldId, found.domains[1].id)
		assertEquals("new.cau.ac.kr", found.domains[1].domain.value)
		assertEquals(2, domainRowCount())
	}

	@Test
	fun `SR4 도메인을 삭제하면 그 행이 DB에서 지워진다`() {
		val saved = adapter.save(newSchool("중앙대학교", "cau.ac.kr", "old.cau.ac.kr"))

		adapter.save(saved.removeDomain(saved.domains[1].id!!))
		val found = reload(saved.id!!)

		assertEquals(listOf("cau.ac.kr"), found.domains.map { it.domain.value })
		assertEquals(1, domainRowCount())
	}

	@Test
	fun `SR5 같은 이름의 학교를 저장하면 DuplicateSchoolNameException이고 cause가 보존된다`() {
		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))

		val thrown = assertFailsWith<DuplicateSchoolNameException> { adapter.save(newSchool("중앙대학교", "other.ac.kr")) }

		assertIs<DataIntegrityViolationException>(thrown.cause)
	}

	@Test
	fun `SR6 다른 학교로 이름을 바꿔 겹쳐도 DuplicateSchoolNameException`() {
		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))
		val other = adapter.save(newSchool("서울대학교", "snu.ac.kr"))

		assertFailsWith<DuplicateSchoolNameException> { adapter.save(other.rename(SchoolName.of("중앙대학교"))) }
	}

	@Test
	fun `SR7 다른 학교가 같은 도메인을 함께 쓸 수 있다 (캠퍼스, Q-S7)`() {
		adapter.save(newSchool("중앙대학교", "cau.ac.kr"))
		adapter.save(newSchool("중앙대학교(다빈치캠퍼스)", "cau.ac.kr"))

		assertEquals(2, domainRowCount())
	}

	@Test
	fun `SR8 없는 id는 null`() {
		assertNull(adapter.findById(999L))
	}

	@Test
	fun `SR9 findAll은 숨긴 학교를 포함해 id 오름차순으로 도메인과 함께 준다`() {
		val a = adapter.save(newSchool("중앙대학교", "cau.ac.kr", "old.cau.ac.kr"))
		val b = adapter.save(newSchool("서울대학교", "snu.ac.kr").changeVisibility(hidden = true))
		entityManager.flush()
		entityManager.clear()

		val all = adapter.findAll()

		assertEquals(listOf(a.id, b.id), all.map { it.id })
		assertEquals(listOf(2, 1), all.map { it.domains.size })
		assertTrue(all[1].hidden)
	}

	@Test
	fun `SR10 학교를 지우면 도메인도 함께 지워지고 없는 id는 무시한다`() {
		val saved = adapter.save(newSchool("중앙대학교", "cau.ac.kr", "old.cau.ac.kr"))

		adapter.deleteById(saved.id!!)
		adapter.deleteById(999L)
		entityManager.flush()
		entityManager.clear()

		assertNull(adapter.findById(saved.id!!))
		assertEquals(0, domainRowCount())
	}
}
