package com.lingo

import com.lingo.adapter.outbound.external.security.JwtTokenProvider
import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
	private val jdbcTemplate: JdbcTemplate,
	@Value("\${lingo.auth.jwt.secret}") private val secret: String,
) {

	@BeforeEach
	fun cleanUsers() {
		jdbcTemplate.update("delete from users")
	}

	private fun json(path: String, body: String, authorization: String? = null): ResultActions =
		mockMvc.perform(
			post(path).contentType(MediaType.APPLICATION_JSON).content(body).apply {
				authorization?.let { header("Authorization", it) }
			},
		)

	private fun signUp(email: String = "a@b.com", password: String = "Abcdef1!"): Long {
		val body = json("/api/v1/auth/signup", """{"email":"$email","password":"$password","name":"홍길동"}""")
			.andExpect(status().isCreated).andReturn().response.contentAsString
		return Regex("\"userId\":(\\d+)").find(body)!!.groupValues[1].toLong()
	}

	private fun login(email: String, password: String, authorization: String? = null) =
		json("/api/v1/auth/login", """{"email":"$email","password":"$password"}""", authorization)

	private fun loginToken(): String {
		val body = login("a@b.com", "Abcdef1!").andReturn().response.contentAsString
		return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
	}

	@Test
	fun `L1 가입한 사용자는 로그인하면 200과 토큰을 받는다`() {
		signUp()

		login("a@b.com", "Abcdef1!")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(3600))
	}

	@Test
	fun `L2 발급된 토큰으로 me에 접근하면 본인 정보를 받는다`() {
		val userId = signUp()

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer ${loginToken()}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andExpect(jsonPath("$.data.email").value("a@b.com"))
	}

	@Test
	fun `L3 틀린 비밀번호와 없는 이메일은 같은 401 응답 본문이다`() {
		signUp()

		val wrongPassword = login("a@b.com", "Wrong1!aa").andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-001")).andReturn().response.contentAsString
		val unknownEmail = login("x@b.com", "Abcdef1!").andExpect(status().isUnauthorized)
			.andReturn().response.contentAsString

		assertEquals(wrongPassword, unknownEmail)
	}

	@Test
	fun `L4 정책 미달 비밀번호와 형식 불량 이메일도 401 AUTH-001이다`() {
		signUp()

		login("a@b.com", "x").andExpect(status().isUnauthorized).andExpect(jsonPath("$.error.code").value("AUTH-001"))
		login("not-an-email", "Abcdef1!").andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-001"))
	}

	@Test
	fun `L5 토큰 없음 위조 만료 토큰은 me에서 401 AUTH-002 JSON이다`() {
		signUp()
		val expired = JwtTokenProvider(secret, 3600, Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC))
			.issue(User(7, Email.of("a@b.com"), "h", "n")).token
		val forged = JwtTokenProvider("another-secret-another-secret-32bytes!", 3600, Clock.systemUTC())
			.issue(User(7, Email.of("a@b.com"), "h", "n")).token

		listOf(null, "Bearer $forged", "Bearer $expired", "Bearer garbage").forEach { header ->
			mockMvc.perform(get("/api/v1/auth/me").apply { header?.let { header("Authorization", it) } })
				.andExpect(status().isUnauthorized)
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH-002"))
		}
	}

	@Test
	fun `L6 permitAll 경로는 쓰레기 Authorization 헤더가 있어도 로그인된다`() {
		signUp()

		login("a@b.com", "Abcdef1!", authorization = "Bearer garbage").andExpect(status().isOk)
	}

	@Test
	fun `L7 로그인 응답에 비밀번호 해시가 없다`() {
		signUp()

		val body = login("a@b.com", "Abcdef1!").andReturn().response.contentAsString

		assertFalse(body.contains("\$2"))
		assertTrue(body.contains("accessToken"))
	}
}
