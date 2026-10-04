package com.lingo.adapter.inbound.web.user

import com.lingo.adapter.inbound.web.common.AuthRequestAttributes
import com.lingo.application.user.AuthenticatedUser
import com.lingo.application.user.DuplicateEmailException
import com.lingo.application.user.InvalidCredentialsException
import com.lingo.application.user.LoginCommand
import com.lingo.application.user.LoginResult
import com.lingo.application.user.LoginService
import com.lingo.application.user.SignUpCommand
import com.lingo.application.user.SignUpResult
import com.lingo.application.user.SignUpService
import com.lingo.domain.user.InvalidEmailException
import com.lingo.domain.user.InvalidPasswordException
import org.hamcrest.Matchers.nullValue
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertFalse

@WebMvcTest(UserController::class)
class UserControllerTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	@MockitoBean
	private lateinit var signUpService: SignUpService

	@MockitoBean
	private lateinit var loginService: LoginService

	private val validBody = """{"email":"a@b.com","password":"Abcdef1!","name":"홍길동"}"""
	private val command = SignUpCommand("a@b.com", "Abcdef1!", "홍길동")

	private fun signUp(body: String) =
		mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))

	@Test
	fun `W1 유효한 요청은 201과 userId email을 반환하고 기대 command로 호출된다`() {
		given(signUpService.signUp(command)).willReturn(SignUpResult(1L, "a@b.com"))

		signUp(validBody)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.email").value("a@b.com"))
			.andExpect(jsonPath("$.error").value(nullValue()))

		verify(signUpService).signUp(command)
	}

	@Test
	fun `W2 이메일 중복은 409 USER-001`() {
		given(signUpService.signUp(command)).willThrow(DuplicateEmailException())

		signUp(validBody)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("USER-001"))
			.andExpect(jsonPath("$.data").value(nullValue()))
	}

	@Test
	fun `W3 비밀번호 정책 위반은 400 COMMON-002이고 응답에 비밀번호 원문이 없다`() {
		given(signUpService.signUp(command)).willThrow(InvalidPasswordException("정책 위반"))

		val body = signUp(validBody)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))
			.andReturn().response.contentAsString

		assertFalse(body.contains("Abcdef1!"))
	}

	@Test
	fun `W3 이메일 형식 위반도 400 COMMON-002`() {
		given(signUpService.signUp(command)).willThrow(InvalidEmailException("형식 위반"))

		signUp(validBody)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))
	}

	@Test
	fun `W4 빈 문자열과 공백 필드는 400이고 서비스를 호출하지 않는다`() {
		listOf(
			"""{"email":"","password":"Abcdef1!","name":"홍길동"}""",
			"""{"email":"a@b.com","password":"   ","name":"홍길동"}""",
			"""{"email":"a@b.com","password":"Abcdef1!","name":" "}""",
		).forEach { body ->
			signUp(body)
				.andExpect(status().isBadRequest)
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("COMMON-001"))
		}

		verifyNoInteractions(signUpService)
	}

	@Test
	fun `W5 필드 누락 깨진 JSON 잘못된 타입은 400 COMMON-002`() {
		listOf(
			"""{"email":"a@b.com","password":"Abcdef1!"}""",
			"""{"email":"a@b.com","password":"Abcdef1!","name":null}""",
			"""{"email":"a@b.com",""",
			"""{"email":["a@b.com"],"password":"Abcdef1!","name":"홍길동"}""",
		).forEach { body ->
			signUp(body)
				.andExpect(status().isBadRequest)
				.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMON-002"))
		}
	}

	@Test
	fun `W6 GET은 405`() {
		mockMvc.perform(get("/api/v1/auth/signup")).andExpect(status().isMethodNotAllowed)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("COMMON-003"))
	}

	private val loginBody = """{"email":"a@b.com","password":"Abcdef1!"}"""
	private val loginCommand = LoginCommand("a@b.com", "Abcdef1!")

	private fun login(body: String) =
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))

	@Test
	fun `W7 로그인 성공은 200과 토큰을 반환하고 기대 command로 호출된다`() {
		given(loginService.login(loginCommand)).willReturn(LoginResult("tkn", 3600))

		login(loginBody)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("tkn"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(3600))
			.andExpect(jsonPath("$.error").value(nullValue()))

		verify(loginService).login(loginCommand)
	}

	@Test
	fun `W8 인증 실패는 401 AUTH-001이고 응답에 비밀번호 원문이 없다`() {
		given(loginService.login(loginCommand)).willThrow(InvalidCredentialsException())

		val body = login(loginBody)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.error.code").value("AUTH-001"))
			.andExpect(jsonPath("$.data").value(nullValue()))
			.andReturn().response.contentAsString

		assertFalse(body.contains("Abcdef1!"))
	}

	@Test
	fun `W9 빈 필드는 400 COMMON-001 누락과 깨진 JSON은 400 COMMON-002이고 서비스를 호출하지 않는다`() {
		listOf(
			"""{"email":"","password":"Abcdef1!"}""",
			"""{"email":"a@b.com","password":"  "}""",
		).forEach { login(it).andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("COMMON-001")) }
		listOf("""{"email":"a@b.com"}""", """{"email":""").forEach {
			login(it).andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("COMMON-002"))
		}

		verifyNoInteractions(loginService)
	}

	@Test
	fun `W10 me는 request attribute의 인증 사용자를 반환한다`() {
		mockMvc.perform(
			get("/api/v1/auth/me")
				.requestAttr(AuthRequestAttributes.AUTHENTICATED_USER, AuthenticatedUser(7, "a@b.com")),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.userId").value(7))
			.andExpect(jsonPath("$.data.email").value("a@b.com"))
			.andExpect(jsonPath("$.data.name").doesNotExist())
	}

	@Test
	fun `W11 GET login은 405 COMMON-003`() {
		mockMvc.perform(get("/api/v1/auth/login")).andExpect(status().isMethodNotAllowed)
			.andExpect(jsonPath("$.error.code").value("COMMON-003"))
	}
}
