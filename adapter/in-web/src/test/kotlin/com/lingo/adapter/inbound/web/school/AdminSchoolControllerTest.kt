package com.lingo.adapter.inbound.web.school

import com.lingo.adapter.inbound.web.common.PermitAllSecurityConfig
import com.lingo.application.school.exception.DuplicateSchoolNameException
import com.lingo.application.school.exception.SchoolDomainNotFoundException
import com.lingo.application.school.exception.SchoolNotFoundException
import com.lingo.application.school.usecase.AddSchoolCommand
import com.lingo.application.school.usecase.AddSchoolDomainCommand
import com.lingo.application.school.usecase.AdminSchoolDomainResult
import com.lingo.application.school.usecase.AdminSchoolResult
import com.lingo.application.school.usecase.AdminSchoolService
import com.lingo.application.school.usecase.ChangeSchoolDomainCommand
import com.lingo.application.school.usecase.UpdateSchoolCommand
import com.lingo.domain.school.LastSchoolDomainException
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(AdminSchoolController::class)
@Import(PermitAllSecurityConfig::class)
class AdminSchoolControllerTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	@MockitoBean
	private lateinit var adminSchoolService: AdminSchoolService

	private val cau = AdminSchoolResult(
		id = 1L,
		name = "중앙대학교",
		hidden = false,
		domains = listOf(AdminSchoolDomainResult(10L, "cau.ac.kr"), AdminSchoolDomainResult(11L, "old.cau.ac.kr")),
	)

	private fun send(request: MockHttpServletRequestBuilder, body: String? = null): ResultActions =
		mockMvc.perform(request.apply { body?.let { contentType(MediaType.APPLICATION_JSON).content(it) } })

	@Test
	fun `WA1 목록은 학교마다 숨김과 도메인 id를 함께 준다`() {
		given(adminSchoolService.listSchools("중앙")).willReturn(listOf(cau))
		given(adminSchoolService.listSchools(null)).willReturn(emptyList())

		send(get("/api/v1/admin/schools").param("query", "중앙"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data[0].id").value(1))
			.andExpect(jsonPath("$.data[0].name").value("중앙대학교"))
			.andExpect(jsonPath("$.data[0].hidden").value(false))
			.andExpect(jsonPath("$.data[0].domains[1].id").value(11))
			.andExpect(jsonPath("$.data[0].domains[1].domain").value("old.cau.ac.kr"))
		send(get("/api/v1/admin/schools")).andExpect(status().isOk).andExpect(jsonPath("$.data.length()").value(0))

		verify(adminSchoolService).listSchools(null)
	}

	@Test
	fun `WA2 학교 추가는 201과 저장된 학교를 준다`() {
		val command = AddSchoolCommand("중앙대학교", listOf("cau.ac.kr", "old.cau.ac.kr"))
		given(adminSchoolService.addSchool(command)).willReturn(cau)

		send(post("/api/v1/admin/schools"), """{"name":"중앙대학교","domains":["cau.ac.kr","old.cau.ac.kr"]}""")
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.domains.length()").value(2))

		verify(adminSchoolService).addSchool(command)
	}

	@Test
	fun `WA3 학교 이름이 비었거나 도메인 목록이 비면 400 COMMON-001`() {
		send(post("/api/v1/admin/schools"), """{"name":" ","domains":[]}""")
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-001"))
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(2))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("domains"))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].field").value("name"))

		verifyNoInteractions(adminSchoolService)
	}

	@Test
	fun `WA4 같은 이름이면 409 SCHOOL-001`() {
		given(adminSchoolService.addSchool(AddSchoolCommand("중앙대학교", listOf("cau.ac.kr")))).willThrow(DuplicateSchoolNameException())

		send(post("/api/v1/admin/schools"), """{"name":"중앙대학교","domains":["cau.ac.kr"]}""")
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.error.code").value("SCHOOL-001"))
	}

	@Test
	fun `WA5 학교 수정은 보낸 값만 넘기고 200`() {
		val command = UpdateSchoolCommand(1L, name = null, hidden = true)
		given(adminSchoolService.updateSchool(command)).willReturn(cau.copy(hidden = true))

		send(patch("/api/v1/admin/schools/1"), """{"hidden":true}""")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.hidden").value(true))

		verify(adminSchoolService).updateSchool(command)
	}

	@Test
	fun `WA6 학교 삭제는 204이고 없는 학교면 404 SCHOOL-002`() {
		send(delete("/api/v1/admin/schools/1"))
			.andExpect(status().isNoContent)
			.andExpect(content().string(""))
		verify(adminSchoolService).deleteSchool(1L)

		given(adminSchoolService.deleteSchool(99L)).willThrow(SchoolNotFoundException())
		send(delete("/api/v1/admin/schools/99"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.error.code").value("SCHOOL-002"))
	}

	@Test
	fun `WA7 도메인 추가 201 수정 200 삭제 204`() {
		given(adminSchoolService.addDomain(AddSchoolDomainCommand(1L, "new.cau.ac.kr"))).willReturn(cau)
		given(adminSchoolService.changeDomain(ChangeSchoolDomainCommand(1L, 11L, "new.cau.ac.kr"))).willReturn(cau)

		send(post("/api/v1/admin/schools/1/domains"), """{"domain":"new.cau.ac.kr"}""")
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.id").value(1))
		send(put("/api/v1/admin/schools/1/domains/11"), """{"domain":"new.cau.ac.kr"}""")
			.andExpect(status().isOk)
		send(delete("/api/v1/admin/schools/1/domains/11"))
			.andExpect(status().isNoContent)

		verify(adminSchoolService).removeDomain(1L, 11L)
	}

	@Test
	fun `WA8 빈 도메인은 400 COMMON-001이고 field는 domain`() {
		send(post("/api/v1/admin/schools/1/domains"), """{"domain":""}""")
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-001"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("domain"))

		verifyNoInteractions(adminSchoolService)
	}

	@Test
	fun `WA9 그 학교에 없는 도메인은 404 SCHOOL-003 마지막 도메인 삭제는 400 COMMON-002`() {
		given(adminSchoolService.removeDomain(1L, 99L)).willThrow(SchoolDomainNotFoundException())
		given(adminSchoolService.removeDomain(1L, 10L)).willThrow(LastSchoolDomainException("학교의 마지막 도메인은 삭제할 수 없습니다."))

		send(delete("/api/v1/admin/schools/1/domains/99"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.error.code").value("SCHOOL-003"))
		send(delete("/api/v1/admin/schools/1/domains/10"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))
			.andExpect(jsonPath("$.error.message").value("학교의 마지막 도메인은 삭제할 수 없습니다."))
	}

	@Test
	fun `WA10 숫자가 아닌 id는 400 COMMON-002`() {
		send(delete("/api/v1/admin/schools/abc"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))

		verifyNoInteractions(adminSchoolService)
	}
}
