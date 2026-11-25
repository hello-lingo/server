package com.lingo

import com.jayway.jsonpath.JsonPath
import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.domain.user.Role
import com.lingo.support.DatabaseCleaner
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSchoolIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
	private val databaseCleaner: DatabaseCleaner,
	jwtProviderPort: JwtProviderPort,
) {

	private val userToken = jwtProviderPort.issue(AuthenticatedUser(1, "user@b.com", Role.USER)).token
	private val adminToken = jwtProviderPort.issue(AuthenticatedUser(2, "admin@b.com", Role.ADMIN)).token

	@BeforeEach
	fun setUp() {
		databaseCleaner.clean()
	}

	private fun send(request: MockHttpServletRequestBuilder, body: String? = null, token: String = adminToken): ResultActions =
		mockMvc.perform(
			request.header("Authorization", "Bearer $token")
				.apply { body?.let { contentType(MediaType.APPLICATION_JSON).content(it) } },
		)

	private fun addSchool(name: String, vararg domains: String): ResultActions =
		send(post("/api/v1/admin/schools"), """{"name":"$name","domains":[${domains.joinToString(",") { "\"$it\"" }}]}""")

	private fun ResultActions.read(path: String): Any = JsonPath.read(andReturn().response.contentAsString, path)

	private fun searchDomains(query: String, token: String = userToken): ResultActions =
		send(get("/api/v1/schools").param("type", "DOMAIN").param("query", query), token = token)

	@Test
	fun `AI1 일반 회원은 학교를 관리할 수 없다 (AC-3_7)`() {
		send(get("/api/v1/admin/schools"), token = userToken).andExpect(status().isForbidden).andExpect(jsonPath("$.error.code").value("AUTH-003"))
		send(post("/api/v1/admin/schools"), """{"name":"중앙대학교","domains":["cau.ac.kr"]}""", token = userToken)
			.andExpect(status().isForbidden)
	}

	@Test
	fun `AI2 관리자가 추가한 학교를 회원이 바로 검색할 수 있다 (AC-3_3)`() {
		addSchool("중앙대학교", "cau.ac.kr")
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.name").value("중앙대학교"))

		searchDomains("cau")
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].schoolName").value("중앙대학교"))
	}

	@Test
	fun `AI3 이름 수정 숨김 도메인 추가 수정 삭제가 검색에 바로 반영된다 (AC-3_5 AC-3_6)`() {
		val schoolId = (addSchool("중앙대학교", "cau.ac.kr").read("$.data.id") as Number).toLong()

		send(patch("/api/v1/admin/schools/$schoolId"), """{"name":"중앙대학교(서울캠퍼스)"}""").andExpect(status().isOk)
		searchDomains("cau").andExpect(jsonPath("$.data[0].schoolName").value("중앙대학교(서울캠퍼스)"))

		val oldDomainId = (send(post("/api/v1/admin/schools/$schoolId/domains"), """{"domain":"old.cau.ac.kr"}""")
			.andExpect(status().isCreated)
			.read("$.data.domains[1].id") as Number).toLong()
		searchDomains("cau").andExpect(jsonPath("$.data.length()").value(2))

		send(put("/api/v1/admin/schools/$schoolId/domains/$oldDomainId"), """{"domain":"legacy.cau.ac.kr"}""").andExpect(status().isOk)
		searchDomains("legacy").andExpect(jsonPath("$.data[0].domainId").value(oldDomainId))

		send(delete("/api/v1/admin/schools/$schoolId/domains/$oldDomainId")).andExpect(status().isNoContent)
		searchDomains("cau").andExpect(jsonPath("$.data.length()").value(1))

		send(patch("/api/v1/admin/schools/$schoolId"), """{"hidden":true}""").andExpect(status().isOk)
		searchDomains("cau").andExpect(jsonPath("$.data.length()").value(0))
		send(get("/api/v1/admin/schools")).andExpect(jsonPath("$.data[0].hidden").value(true))
	}

	@Test
	fun `AI4 같은 이름은 409 SCHOOL-001이고 같은 도메인을 쓰는 캠퍼스는 추가할 수 있다`() {
		addSchool("중앙대학교", "cau.ac.kr").andExpect(status().isCreated)

		addSchool("중앙대학교", "other.ac.kr").andExpect(status().isConflict).andExpect(jsonPath("$.error.code").value("SCHOOL-001"))
		addSchool("중앙대학교(다빈치캠퍼스)", "cau.ac.kr").andExpect(status().isCreated)

		searchDomains("cau").andExpect(jsonPath("$.data.length()").value(2))
	}

	@Test
	fun `AI5 마지막 도메인 삭제는 400 없는 학교는 404 학교 삭제 뒤에는 검색되지 않는다`() {
		val created = addSchool("중앙대학교", "cau.ac.kr")
		val schoolId = (created.read("$.data.id") as Number).toLong()
		val domainId = (created.read("$.data.domains[0].id") as Number).toLong()

		send(delete("/api/v1/admin/schools/$schoolId/domains/$domainId"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))
		send(delete("/api/v1/admin/schools/999")).andExpect(status().isNotFound).andExpect(jsonPath("$.error.code").value("SCHOOL-002"))

		send(delete("/api/v1/admin/schools/$schoolId")).andExpect(status().isNoContent)
		searchDomains("cau").andExpect(jsonPath("$.data.length()").value(0))
	}

	@Test
	fun `AI6 형식이 틀린 도메인은 400 COMMON-002이고 저장되지 않는다`() {
		addSchool("중앙대학교", "user@cau.ac.kr").andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("COMMON-002"))

		send(get("/api/v1/admin/schools")).andExpect(jsonPath("$.data.length()").value(0))
	}
}
