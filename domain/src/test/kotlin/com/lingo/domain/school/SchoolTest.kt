package com.lingo.domain.school

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchoolTest {

	private val cau = SchoolDomain.of("cau.ac.kr")
	private val oldCau = SchoolDomain.of("old.cau.ac.kr")
	private val name = SchoolName.of("중앙대학교")

	/** DB에서 읽어 온 상태처럼 id가 있는 학교 */
	private fun saved(vararg domains: Pair<Long, SchoolDomain>, hidden: Boolean = false) = School(
		id = 1L,
		name = name,
		domains = domains.map { (id, domain) -> RegisteredDomain(id, domain) },
		hidden = hidden,
	)

	@Test
	fun `SC1 create는 id 없이 숨기지 않은 학교를 만든다`() {
		val school = School.create(name, listOf(cau, oldCau))

		assertNull(school.id)
		assertEquals(name, school.name)
		assertEquals(listOf(cau, oldCau), school.domains.map { it.domain })
		assertTrue(school.domains.all { it.id == null })
		assertFalse(school.hidden)
	}

	@Test
	fun `SC2 도메인 없이는 만들 수 없다`() {
		assertFailsWith<SchoolDomainRequiredException> { School.create(name, emptyList()) }
		assertFailsWith<SchoolDomainRequiredException> { School(1L, name, emptyList(), false) }
	}

	@Test
	fun `SC3 같은 도메인을 두 번 넣을 수 없다`() {
		assertFailsWith<DuplicateDomainInSchoolException> { School.create(name, listOf(cau, SchoolDomain.of("CAU.ac.kr"))) }
	}

	@Test
	fun `SC4 이름을 바꾼다`() {
		val renamed = saved(10L to cau).rename(SchoolName.of("중앙대학교(다빈치캠퍼스)"))

		assertEquals("중앙대학교(다빈치캠퍼스)", renamed.name.value)
		assertEquals(1L, renamed.id)
	}

	@Test
	fun `SC5 숨기고 다시 보이게 한다`() {
		val hidden = saved(10L to cau).changeVisibility(hidden = true)
		assertTrue(hidden.hidden)
		assertFalse(hidden.changeVisibility(hidden = false).hidden)
	}

	@Test
	fun `SC6 도메인을 추가하면 id 없는 도메인이 뒤에 붙는다`() {
		val school = saved(10L to cau).addDomain(oldCau)

		assertEquals(listOf(RegisteredDomain(10L, cau), RegisteredDomain(null, oldCau)), school.domains)
	}

	@Test
	fun `SC7 이미 있는 도메인은 추가할 수 없다`() {
		assertFailsWith<DuplicateDomainInSchoolException> { saved(10L to cau).addDomain(SchoolDomain.of(" CAU.AC.KR")) }
	}

	@Test
	fun `SC8 도메인 값을 바꾸면 id는 그대로다`() {
		val school = saved(10L to cau, 11L to oldCau).changeDomain(11L, SchoolDomain.of("new.cau.ac.kr"))

		assertEquals(listOf(RegisteredDomain(10L, cau), RegisteredDomain(11L, SchoolDomain.of("new.cau.ac.kr"))), school.domains)
	}

	@Test
	fun `SC9 같은 학교의 다른 도메인 값으로 바꿀 수 없다`() {
		assertFailsWith<DuplicateDomainInSchoolException> { saved(10L to cau, 11L to oldCau).changeDomain(11L, cau) }
	}

	@Test
	fun `SC10 자기 값 그대로 바꾸는 것은 허용한다`() {
		val school = saved(10L to cau).changeDomain(10L, cau)
		assertEquals(listOf(RegisteredDomain(10L, cau)), school.domains)
	}

	@Test
	fun `SC11 도메인을 삭제한다`() {
		val school = saved(10L to cau, 11L to oldCau).removeDomain(10L)
		assertEquals(listOf(RegisteredDomain(11L, oldCau)), school.domains)
	}

	@Test
	fun `SC12 마지막 도메인은 삭제할 수 없다`() {
		assertFailsWith<LastSchoolDomainException> { saved(10L to cau).removeDomain(10L) }
	}

	@Test
	fun `SC13 hasDomain은 도메인 id가 이 학교에 있는지 알려 준다`() {
		val school = saved(10L to cau)
		assertTrue(school.hasDomain(10L))
		assertFalse(school.hasDomain(99L))
	}

	@Test
	fun `SC14 없는 도메인 id로 바꾸거나 지우면 IllegalArgumentException (서비스가 hasDomain으로 먼저 확인한다)`() {
		val school = saved(10L to cau, 11L to oldCau)
		assertFailsWith<IllegalArgumentException> { school.changeDomain(99L, SchoolDomain.of("x.ac.kr")) }
		assertFailsWith<IllegalArgumentException> { school.removeDomain(99L) }
	}
}
