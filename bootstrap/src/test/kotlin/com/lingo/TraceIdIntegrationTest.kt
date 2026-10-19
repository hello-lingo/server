package com.lingo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// 로그인 실패·인증 실패만 쓰므로 DB에 쓰지 않는다 (DatabaseCleaner 불필요)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TraceIdIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	private fun loginWithUnknownEmail(): ResultActions =
		mockMvc.perform(
			post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"nobody@b.com","password":"Abcdef1!"}"""),
		).andExpect(status().isUnauthorized).andExpect(jsonPath("$.error.code").value("AUTH-001"))

	private fun ResultActions.traceId(): String? = andReturn().response.getHeader(TRACE_ID_HEADER)

	@Test
	fun `TR1 로그인 실패 401 응답에 X-Trace-Id가 있고 16진수 16자 또는 32자다`() {
		val traceId = loginWithUnknownEmail().traceId()

		assertNotNull(traceId, "$TRACE_ID_HEADER 헤더가 없다")
		assertTrue(TRACE_ID_PATTERN.matches(traceId), "형식이 맞지 않는다: $traceId")
	}

	@Test
	fun `TR2 보안 필터에서 끝나는 401 응답에도 X-Trace-Id가 있다`() {
		val traceId = mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-002"))
			.traceId()

		assertNotNull(traceId, "$TRACE_ID_HEADER 헤더가 없다")
		assertTrue(TRACE_ID_PATTERN.matches(traceId), "형식이 맞지 않는다: $traceId")
	}

	@Test
	fun `TR3 요청마다 X-Trace-Id 값이 다르다`() {
		val first = loginWithUnknownEmail().traceId()
		val second = loginWithUnknownEmail().traceId()

		assertNotNull(first, "$TRACE_ID_HEADER 헤더가 없다")
		assertNotNull(second, "$TRACE_ID_HEADER 헤더가 없다")
		assertNotEquals(first, second)
	}

	private companion object {
		// 구현 상수(TraceIdResponseFilter.TRACE_ID_HEADER)를 참조하지 않고 앱과의 계약 값을 그대로 고정한다
		const val TRACE_ID_HEADER = "X-Trace-Id"
		val TRACE_ID_PATTERN = Regex("^[0-9a-f]{16}([0-9a-f]{16})?$")
	}
}
