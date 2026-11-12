package com.lingo.adapter.inbound.web.school

import com.lingo.adapter.inbound.web.common.PermitAllSecurityConfig
import com.lingo.application.school.usecase.SchoolSearchResult
import com.lingo.application.school.usecase.SchoolSearchType
import com.lingo.application.school.usecase.SearchSchoolsQuery
import com.lingo.application.school.usecase.SearchSchoolsService
import org.hamcrest.Matchers.nullValue
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(SchoolController::class)
@Import(PermitAllSecurityConfig::class)
class SchoolControllerTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	@MockitoBean
	private lateinit var searchSchoolsService: SearchSchoolsService

	private fun search(vararg params: Pair<String, String>): ResultActions =
		mockMvc.perform(get("/api/v1/schools").apply { params.forEach { (k, v) -> param(k, v) } })

	@Test
	fun `WS1 학교 이름 검색은 200과 학교–도메인 줄 목록을 준다`() {
		val query = SearchSchoolsQuery(SchoolSearchType.NAME, "중앙")
		given(searchSchoolsService.search(query)).willReturn(
			listOf(
				SchoolSearchResult(1L, "중앙대학교", 10L, "cau.ac.kr"),
				SchoolSearchResult(1L, "중앙대학교", 11L, "old.cau.ac.kr"),
			),
		)

		search("type" to "NAME", "query" to "중앙")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].schoolId").value(1))
			.andExpect(jsonPath("$.data[0].schoolName").value("중앙대학교"))
			.andExpect(jsonPath("$.data[0].domainId").value(10))
			.andExpect(jsonPath("$.data[0].domain").value("cau.ac.kr"))
			.andExpect(jsonPath("$.data[1].domain").value("old.cau.ac.kr"))
			.andExpect(jsonPath("$.error").value(nullValue()))

		verify(searchSchoolsService).search(query)
	}

	@Test
	fun `WS2 도메인 검색은 검색어를 그대로 서비스에 넘긴다 (정리는 서비스가 한다)`() {
		val query = SearchSchoolsQuery(SchoolSearchType.DOMAIN, " CA ")
		given(searchSchoolsService.search(query)).willReturn(emptyList())

		search("type" to "DOMAIN", "query" to " CA ")
			.andExpect(status().isOk)

		verify(searchSchoolsService).search(query)
	}

	@Test
	fun `WS3 결과가 없으면 빈 배열 200 (AC-1_11 안내는 앱이 띄운다)`() {
		given(searchSchoolsService.search(SearchSchoolsQuery(SchoolSearchType.NAME, "없는학교"))).willReturn(emptyList())

		search("type" to "NAME", "query" to "없는학교")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.length()").value(0))
	}

	@Test
	fun `WS4 공백뿐인 검색어는 400 COMMON-001이고 field는 query`() {
		search("type" to "NAME", "query" to "   ")
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-001"))
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(1))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("query"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("필수 입력 항목입니다."))

		verifyNoInteractions(searchSchoolsService)
	}

	@Test
	fun `WS5 검색어가 253자를 넘으면 400 COMMON-001`() {
		search("type" to "DOMAIN", "query" to "a".repeat(254))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-001"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("query"))

		verifyNoInteractions(searchSchoolsService)
	}

	@Test
	fun `WS6 검색어나 검색 방식이 없으면 400 COMMON-002`() {
		search("type" to "NAME").andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("COMMON-002"))
		search("query" to "중앙").andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("COMMON-002"))

		verifyNoInteractions(searchSchoolsService)
	}

	@Test
	fun `WS7 검색 방식은 NAME DOMAIN만 받는다 (소문자 포함 그 밖의 값은 400 COMMON-002)`() {
		listOf("name", "SCHOOL", "").forEach { type ->
			search("type" to type, "query" to "중앙")
				.andExpect(status().isBadRequest)
				.andExpect(jsonPath("$.error.code").value("COMMON-002"))
		}

		verifyNoInteractions(searchSchoolsService)
	}
}
