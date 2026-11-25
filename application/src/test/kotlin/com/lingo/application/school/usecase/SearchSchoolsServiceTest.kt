package com.lingo.application.school.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchSchoolsServiceTest {

	private val store = FakeSchoolStore()
	private val service = SearchSchoolsService(store)

	private fun byName(keyword: String) = service.search(SearchSchoolsQuery(SchoolSearchType.NAME, keyword))

	private fun byDomain(keyword: String) = service.search(SearchSchoolsQuery(SchoolSearchType.DOMAIN, keyword))

	/** 결과를 "학교 이름 — 도메인" 줄로 */
	private fun List<SchoolSearchResult>.lines() = map { "${it.schoolName} — ${it.domain}" }

	@Test
	fun `SS1 학교 이름 검색은 입력한 문자열이 이름에 포함된 학교만 남긴다 (AC-1_7)`() {
		store.given("중앙대학교", "cau.ac.kr")
		store.given("중앙가상대학교", "example.ac.kr")
		store.given("서울대학교", "snu.ac.kr")

		assertEquals(listOf("중앙가상대학교", "중앙대학교"), byName("중").map { it.schoolName })
		assertEquals(listOf("중앙가상대학교", "중앙대학교"), byName("중앙").map { it.schoolName })
		assertEquals(listOf("중앙대학교 — cau.ac.kr"), byName("중앙대").lines())
	}

	@Test
	fun `SS2 도메인 검색은 입력한 문자열이 도메인에 포함된 줄만 남긴다 (AC-1_8)`() {
		store.given("중앙대학교", "cau.ac.kr")
		store.given("가톨릭대학교", "cuk.ac.kr")
		store.given("서울대학교", "snu.ac.kr")

		// 모든 .ac.kr 도메인에 c가 들어 있으므로 snu.ac.kr도 포함 일치로 뒤에 나온다
		assertEquals(listOf("cau.ac.kr", "cuk.ac.kr", "snu.ac.kr"), byDomain("c").map { it.domain })
		assertEquals(listOf("cau.ac.kr"), byDomain("ca").map { it.domain })
	}

	@Test
	fun `SS3 결과는 학교와 도메인의 id를 함께 준다 (AC-1_9 자동 입력용)`() {
		val cau = store.given("중앙대학교", "cau.ac.kr")

		val result = byDomain("cau").single()

		assertEquals(SchoolSearchResult(cau.id!!, "중앙대학교", cau.domains.single().id!!, "cau.ac.kr"), result)
	}

	@Test
	fun `SS4 도메인이 여러 개인 학교는 이름 검색에서 도메인마다 한 줄씩 나온다 (AC-1_10)`() {
		store.given("중앙대학교", "cau.ac.kr", "old.cau.ac.kr")

		assertEquals(listOf("중앙대학교 — cau.ac.kr", "중앙대학교 — old.cau.ac.kr"), byName("중앙").lines())
	}

	@Test
	fun `SS5 같은 도메인을 쓰는 캠퍼스는 도메인 검색에서 학교마다 한 줄씩 이름순으로 나온다 (Q-S7)`() {
		store.given("중앙대학교(다빈치캠퍼스)", "cau.ac.kr")
		store.given("중앙대학교", "cau.ac.kr")

		assertEquals(listOf("중앙대학교 — cau.ac.kr", "중앙대학교(다빈치캠퍼스) — cau.ac.kr"), byDomain("cau").lines())
	}

	@Test
	fun `SS6 대소문자와 앞뒤 공백은 무시한다 (AC-1_12)`() {
		store.given("중앙대학교", "cau.ac.kr")
		store.given("KAIST", "kaist.ac.kr")

		assertEquals(listOf("cau.ac.kr"), byDomain(" CAU ").map { it.domain })
		assertEquals(listOf("KAIST"), byName(" kai ").map { it.schoolName })
	}

	@Test
	fun `SS7 도메인 검색은 입력값으로 시작하는 줄이 먼저 그다음 중간에 포함한 줄 각각 오름차순 (AC-1_13)`() {
		store.given("국제대학교", "kcu.ac.kr")
		store.given("가톨릭대학교", "cuk.ac.kr")
		store.given("중앙대학교", "cau.ac.kr")
		store.given("청주대학교", "ccu.ac.kr")

		assertEquals(listOf("cau.ac.kr", "ccu.ac.kr", "cuk.ac.kr", "kcu.ac.kr"), byDomain("c").map { it.domain })
	}

	@Test
	fun `SS8 학교 이름 검색도 같은 규칙으로 가나다순 (AC-1_13)`() {
		store.given("한중대학교", "hj.ac.kr")
		store.given("중앙대학교", "cau.ac.kr")
		store.given("가중대학교", "gj.ac.kr")
		store.given("중부대학교", "joongbu.ac.kr")

		assertEquals(listOf("중부대학교", "중앙대학교", "가중대학교", "한중대학교"), byName("중").map { it.schoolName })
	}

	@Test
	fun `SS9 숨긴 학교는 검색되지 않는다 (AC-3_6)`() {
		store.given("중앙대학교", "cau.ac.kr")
		store.given("서남대학교", "seonam.ac.kr", hidden = true)

		assertEquals(listOf("중앙대학교"), byName("대학교").map { it.schoolName })
		assertTrue(byDomain("seonam").isEmpty())
	}

	@Test
	fun `SS10 정렬한 뒤 앞에서 20줄만 준다 (NFR-2_3)`() {
		// 시작 일치 묶음이 뒤에 저장돼도 앞에 와야 한다: 포함 일치 15줄을 먼저, 시작 일치 10줄을 나중에 저장
		(1..15).forEach { store.given("포함%02d대학교".format(it), "x$it.ac.kr") }
		(1..10).forEach { store.given("대학%02d".format(it), "y$it.ac.kr") }

		val result = byName("대학")

		assertEquals(20, result.size)
		assertEquals((1..10).map { "대학%02d".format(it) }, result.take(10).map { it.schoolName })
		assertEquals((1..10).map { "포함%02d대학교".format(it) }, result.drop(10).map { it.schoolName })
	}

	@Test
	fun `SS11 이름 검색은 도메인을 보지 않고 도메인 검색은 이름을 보지 않는다`() {
		store.given("중앙대학교", "cau.ac.kr")

		assertTrue(byName("cau").isEmpty())
		assertTrue(byDomain("중앙").isEmpty())
	}

	@Test
	fun `SS12 정리 후 빈 검색어는 저장소를 보지 않고 빈 목록 (빈 값 거부는 웹 계층이 한다)`() {
		store.given("중앙대학교", "cau.ac.kr")

		assertTrue(byName("   ").isEmpty())
		assertEquals(0, store.findAllCalls)
	}
}
