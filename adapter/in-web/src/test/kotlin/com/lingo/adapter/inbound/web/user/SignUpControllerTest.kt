package com.lingo.adapter.inbound.web.user

import com.lingo.application.user.DuplicateEmailException
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

@WebMvcTest(SignUpController::class)
class SignUpControllerTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	@MockitoBean
	private lateinit var signUpService: SignUpService

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
}
