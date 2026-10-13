package com.lingo

import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.domain.user.Role
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.nullValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import testsupport.AdminProbeController
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminProbeController::class)
class AdminAuthorizationIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
	private val jwtProviderPort: JwtProviderPort,
) {

	// 인증 필터는 DB를 조회하지 않으므로 사용자 행이 없어도 된다
	private val userToken = jwtProviderPort.issue(AuthenticatedUser(1, "user@b.com", Role.USER)).token
	private val adminToken = jwtProviderPort.issue(AuthenticatedUser(2, "admin@b.com", Role.ADMIN)).token

	private fun getWith(path: String, token: String? = null): ResultActions =
		mockMvc.perform(get(path).apply { token?.let { header("Authorization", "Bearer $it") } })

	@Test
	fun `AZ1 USER 토큰으로 관리자 경로에 접근하면 403 AUTH-003 JSON이다`() {
		getWith("/api/v1/admin/probe", userToken)
			.andExpect(status().isForbidden)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"))
			.andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다."))
			.andExpect(jsonPath("$.error").value(hasKey("details")))
			.andExpect(jsonPath("$.error.details").value(nullValue()))
	}

	@Test
	fun `AZ2 ADMIN 토큰으로 관리자 경로에 접근하면 200이다`() {
		getWith("/api/v1/admin/probe", adminToken)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data").value("ok"))
	}

	@Test
	fun `AZ3 토큰 없이 관리자 경로에 접근하면 401 AUTH-002 JSON이다`() {
		getWith("/api/v1/admin/probe")
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-002"))
			.andExpect(jsonPath("$.error").value(hasKey("details")))
			.andExpect(jsonPath("$.error.details").value(nullValue()))
	}

	@Test
	fun `AZ4 USER 토큰으로 관리자 외 인증 경로에 접근하면 200이다`() {
		getWith("/api/v1/auth/me", userToken)
			.andExpect(status().isOk)
	}
}
