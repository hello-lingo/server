package com.lingo

import com.lingo.application.school.port.out.SchoolRepositoryPort
import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName
import com.lingo.domain.user.Role
import com.lingo.support.DatabaseCleaner
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

/** 통합 테스트는 시드 데이터에 기대지 않는다. DatabaseCleaner가 시드까지 지우므로 필요한 학교를 직접 저장한다 (04 R1) */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolSearchIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
	private val databaseCleaner: DatabaseCleaner,
	private val schoolRepositoryPort: SchoolRepositoryPort,
	jwtProviderPort: JwtProviderPort,
) {

	private val userToken = jwtProviderPort.issue(AuthenticatedUser(1, "user@b.com", Role.USER)).token

	@BeforeEach
	fun setUp() {
		databaseCleaner.clean()
	}

	private fun given(name: String, vararg domains: String, hidden: Boolean = false) =
		schoolRepositoryPort.save(School.create(SchoolName.of(name), domains.map(SchoolDomain::of)).changeVisibility(hidden))

	private fun search(type: String, query: String, token: String? = userToken): ResultActions =
		mockMvc.perform(
			get("/api/v1/schools").param("type", type).param("query", query)
				.apply { token?.let { header("Authorization", "Bearer $it") } },
		)

	@Test
	fun `SI1 로그인한 회원은 학교 이름으로 검색해 학교–도메인 줄을 받는다`() {
		given("중앙대학교", "cau.ac.kr")
		given("중앙대학교(다빈치캠퍼스)", "cau.ac.kr")
		given("서울대학교", "snu.ac.kr")
		given("중앙가상대학교", "example.ac.kr", hidden = true)

		search("NAME", "중앙")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].schoolName").value("중앙대학교"))
			.andExpect(jsonPath("$.data[0].domain").value("cau.ac.kr"))
			.andExpect(jsonPath("$.data[1].schoolName").value("중앙대학교(다빈치캠퍼스)"))
	}

	@Test
	fun `SI2 학교를 바꾸면 바로 다음 검색에 반영된다 (캐시 비움)`() {
		val cau = given("중앙대학교", "cau.ac.kr")
		search("DOMAIN", "cau").andExpect(jsonPath("$.data.length()").value(1))

		schoolRepositoryPort.save(cau.changeVisibility(hidden = true))

		search("DOMAIN", "cau").andExpect(jsonPath("$.data.length()").value(0))
	}

	@Test
	fun `SI3 토큰 없이 검색하면 401 AUTH-002`() {
		search("NAME", "중앙", token = null)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-002"))
	}
}
