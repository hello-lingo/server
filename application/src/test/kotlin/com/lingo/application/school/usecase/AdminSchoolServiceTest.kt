package com.lingo.application.school.usecase

import com.lingo.application.school.exception.DuplicateSchoolNameException
import com.lingo.application.school.exception.SchoolDomainNotFoundException
import com.lingo.application.school.exception.SchoolNotFoundException
import com.lingo.domain.school.InvalidSchoolDomainException
import com.lingo.domain.school.InvalidSchoolNameException
import com.lingo.domain.school.LastSchoolDomainException
import com.lingo.domain.school.SchoolDomainRequiredException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminSchoolServiceTest {

	private val store = FakeSchoolStore()
	private val service = AdminSchoolService(store)

	private fun AdminSchoolResult.domainValues() = domains.map { it.domain }

	// --- 학교 추가 (AC-3.3, AC-3.4) ---

	@Test
	fun `AS1 이름과 도메인으로 학교를 추가하면 정리된 값으로 저장되고 숨기지 않은 상태다`() {
		val result = service.addSchool(AddSchoolCommand(" 중앙대학교 ", listOf(" CAU.ac.kr", "old.cau.ac.kr")))

		assertEquals("중앙대학교", result.name)
		assertEquals(listOf("cau.ac.kr", "old.cau.ac.kr"), result.domainValues())
		assertFalse(result.hidden)
		assertEquals(result.id, store.schools.single().id)
		assertTrue(result.domains.all { it.id > 0 })
	}

	@Test
	fun `AS2 도메인 없이는 학교를 추가할 수 없다 (AC-3_4)`() {
		assertFailsWith<SchoolDomainRequiredException> { service.addSchool(AddSchoolCommand("중앙대학교", emptyList())) }
		assertTrue(store.schools.isEmpty())
	}

	@Test
	fun `AS3 이름이나 도메인 형식이 틀리면 저장하지 않는다`() {
		assertFailsWith<InvalidSchoolNameException> { service.addSchool(AddSchoolCommand(" ", listOf("cau.ac.kr"))) }
		assertFailsWith<InvalidSchoolDomainException> { service.addSchool(AddSchoolCommand("중앙대학교", listOf("user@cau.ac.kr"))) }
		assertTrue(store.schools.isEmpty())
	}

	@Test
	fun `AS4 같은 이름의 학교는 추가할 수 없다 (Q-S4)`() {
		store.given("중앙대학교", "cau.ac.kr")

		assertFailsWith<DuplicateSchoolNameException> { service.addSchool(AddSchoolCommand("중앙대학교", listOf("other.ac.kr"))) }
	}

	@Test
	fun `AS5 다른 학교가 쓰는 도메인으로도 추가할 수 있다 (캠퍼스, Q-S7)`() {
		store.given("중앙대학교", "cau.ac.kr")

		val result = service.addSchool(AddSchoolCommand("중앙대학교(다빈치캠퍼스)", listOf("cau.ac.kr")))

		assertEquals(listOf("cau.ac.kr"), result.domainValues())
	}

	// --- 이름 수정·숨김 (AC-3.5, AC-3.6) ---

	@Test
	fun `AS6 이름을 바꾸고 숨길 수 있다`() {
		val school = store.given("중앙대학교", "cau.ac.kr")

		val renamed = service.updateSchool(UpdateSchoolCommand(school.id!!, name = "중앙대학교(서울캠퍼스)", hidden = null))
		assertEquals("중앙대학교(서울캠퍼스)", renamed.name)
		assertFalse(renamed.hidden)

		val hidden = service.updateSchool(UpdateSchoolCommand(school.id!!, name = null, hidden = true))
		assertEquals("중앙대학교(서울캠퍼스)", hidden.name)
		assertTrue(hidden.hidden)
		assertTrue(store.findById(school.id!!)!!.hidden)
	}

	@Test
	fun `AS7 다른 학교의 이름으로 바꿀 수 없다`() {
		store.given("중앙대학교", "cau.ac.kr")
		val snu = store.given("서울대학교", "snu.ac.kr")

		assertFailsWith<DuplicateSchoolNameException> { service.updateSchool(UpdateSchoolCommand(snu.id!!, name = "중앙대학교", hidden = null)) }
	}

	@Test
	fun `AS8 바꿀 값이 없으면 그대로 돌려준다`() {
		val school = store.given("중앙대학교", "cau.ac.kr")

		assertEquals("중앙대학교", service.updateSchool(UpdateSchoolCommand(school.id!!, name = null, hidden = null)).name)
	}

	// --- 학교 삭제 (AC-3.6) ---

	@Test
	fun `AS9 학교를 삭제한다`() {
		val school = store.given("중앙대학교", "cau.ac.kr")

		service.deleteSchool(school.id!!)

		assertNull(store.findById(school.id!!))
	}

	// --- 도메인 추가·수정·삭제 (AC-3.5, AC-3.6) ---

	@Test
	fun `AS10 도메인을 추가한다`() {
		val school = store.given("중앙대학교", "cau.ac.kr")

		val result = service.addDomain(AddSchoolDomainCommand(school.id!!, " OLD.cau.ac.kr "))

		assertEquals(listOf("cau.ac.kr", "old.cau.ac.kr"), result.domainValues())
	}

	@Test
	fun `AS11 도메인 값을 바꾸면 도메인 id는 그대로다`() {
		val school = store.given("중앙대학교", "cau.ac.kr", "old.cau.ac.kr")
		val oldId = school.domains[1].id!!

		val result = service.changeDomain(ChangeSchoolDomainCommand(school.id!!, oldId, "new.cau.ac.kr"))

		assertEquals(oldId, result.domains[1].id)
		assertEquals("new.cau.ac.kr", result.domains[1].domain)
	}

	@Test
	fun `AS12 도메인을 삭제하되 마지막 도메인은 삭제할 수 없다`() {
		val school = store.given("중앙대학교", "cau.ac.kr", "old.cau.ac.kr")

		val result = service.removeDomain(school.id!!, school.domains[1].id!!)
		assertEquals(listOf("cau.ac.kr"), result.domainValues())

		assertFailsWith<LastSchoolDomainException> { service.removeDomain(school.id!!, school.domains[0].id!!) }
		assertEquals(1, store.findById(school.id!!)!!.domains.size)
	}

	// --- 없는 대상 (404) ---

	@Test
	fun `AS13 없는 학교는 SchoolNotFoundException`() {
		assertFailsWith<SchoolNotFoundException> { service.updateSchool(UpdateSchoolCommand(99L, name = "x", hidden = null)) }
		assertFailsWith<SchoolNotFoundException> { service.deleteSchool(99L) }
		assertFailsWith<SchoolNotFoundException> { service.addDomain(AddSchoolDomainCommand(99L, "x.ac.kr")) }
		assertFailsWith<SchoolNotFoundException> { service.changeDomain(ChangeSchoolDomainCommand(99L, 1L, "x.ac.kr")) }
		assertFailsWith<SchoolNotFoundException> { service.removeDomain(99L, 1L) }
	}

	@Test
	fun `AS14 그 학교에 없는 도메인 id는 SchoolDomainNotFoundException (다른 학교의 도메인 id 포함)`() {
		val cau = store.given("중앙대학교", "cau.ac.kr", "old.cau.ac.kr")
		val snu = store.given("서울대학교", "snu.ac.kr")
		val snuDomainId = snu.domains.single().id!!

		assertFailsWith<SchoolDomainNotFoundException> { service.changeDomain(ChangeSchoolDomainCommand(cau.id!!, snuDomainId, "x.ac.kr")) }
		assertFailsWith<SchoolDomainNotFoundException> { service.removeDomain(cau.id!!, snuDomainId) }
	}

	// --- 관리자용 목록 ---

	@Test
	fun `AS15 목록은 숨긴 학교를 포함해 이름순으로 도메인을 묶어 준다`() {
		store.given("중앙대학교", "cau.ac.kr", "old.cau.ac.kr")
		store.given("서울대학교", "snu.ac.kr", hidden = true)

		val all = service.listSchools(query = null)

		assertEquals(listOf("서울대학교", "중앙대학교"), all.map { it.name })
		assertTrue(all[0].hidden)
		assertEquals(listOf("cau.ac.kr", "old.cau.ac.kr"), all[1].domainValues())
	}

	@Test
	fun `AS16 검색어가 있으면 이름이나 도메인에 포함된 학교만 준다 (대소문자 공백 무시)`() {
		store.given("중앙대학교", "cau.ac.kr")
		store.given("서울대학교", "snu.ac.kr")

		assertEquals(listOf("중앙대학교"), service.listSchools(" 중앙 ").map { it.name })
		assertEquals(listOf("서울대학교"), service.listSchools("SNU").map { it.name })
		assertEquals(2, service.listSchools("  ").size)
	}
}
