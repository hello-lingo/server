package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.UserErrorType
import com.lingo.application.user.exception.DuplicateEmailException
import com.lingo.application.user.exception.InvalidCredentialsException
import com.lingo.domain.user.InvalidEmailException
import com.lingo.domain.user.InvalidNameException
import com.lingo.domain.user.InvalidPasswordException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.nullValue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class ProbeBody(@field:NotBlank val name: String)

class DetailedProbeException : ApplicationException(UserErrorType.DUPLICATE_EMAIL, debugMessage = "내부-디버그-메모") {
	override val details = mapOf("remainingAttempts" to 4)
}

class CausedProbeException : ApplicationException(UserErrorType.DUPLICATE_EMAIL, cause = IllegalStateException("원인-예외"))

@RestController
class ExceptionProbeController {

	@GetMapping("/probe/throw")
	fun throwing(@RequestParam type: String): String = throw when (type) {
		"duplicate" -> DuplicateEmailException()
		"credentials" -> InvalidCredentialsException()
		"password" -> InvalidPasswordException("정책 위반")
		"email" -> InvalidEmailException("이메일 위반")
		"name" -> InvalidNameException("이름 위반")
		"detailed" -> DetailedProbeException()
		"caused" -> CausedProbeException()
		else -> IllegalStateException("secret-internal-detail")
	}

	@PostMapping("/probe/body", consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun body(@Valid @RequestBody body: ProbeBody): String = body.name
}

@WebMvcTest(ExceptionProbeController::class)
@ExtendWith(OutputCaptureExtension::class)
@Import(PermitAllSecurityConfig::class)
class GlobalExceptionHandlerTest @Autowired constructor(
	private val mockMvc: MockMvc,
) {

	private fun probe(type: String) = mockMvc.perform(get("/probe/throw").param("type", type))

	private fun ResultActions.expectError(status: Int, code: String, message: String? = null): ResultActions {
		andExpect(status().`is`(status))
		andExpect(jsonPath("$.success").value(false))
		andExpect(jsonPath("$.data").value(nullValue()))
		andExpect(jsonPath("$.error.code").value(code))
		message?.let { andExpect(jsonPath("$.error.message").value(it)) }
		return this
	}

	@Test
	fun `H1 이메일 중복은 409 USER-001`() {
		probe("duplicate").expectError(409, "USER-001", "이미 가입된 이메일입니다.")
	}

	@Test
	fun `H1-2 인증 실패는 401 AUTH-001`() {
		probe("credentials").expectError(401, "AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다.")
	}

	@Test
	fun `H2 도메인 예외는 400 COMMON-002이고 예외 메시지를 쓴다`() {
		probe("password").expectError(400, "COMMON-002", "정책 위반")
	}

	@Test
	fun `H3 도메인 예외 하위 타입 전부 400 COMMON-002`() {
		probe("email").expectError(400, "COMMON-002", "이메일 위반")
		probe("name").expectError(400, "COMMON-002", "이름 위반")
	}

	@Test
	fun `H4 Bean Validation 실패는 400 COMMON-001`() {
		mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))
			.expectError(400, "COMMON-001")
	}

	@Test
	fun `H5 깨진 JSON은 400 COMMON-002`() {
		mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("""{"name":"""))
			.expectError(400, "COMMON-002")
	}

	@Test
	fun `H6 허용되지 않는 메서드는 405 COMMON-003`() {
		mockMvc.perform(get("/probe/body")).expectError(405, "COMMON-003")
	}

	@Test
	fun `H7 지원하지 않는 Content-Type은 415 COMMON-004`() {
		mockMvc.perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("x"))
			.expectError(415, "COMMON-004")
	}

	@Test
	fun `H8 존재하지 않는 경로는 404 COMMON-005`() {
		mockMvc.perform(get("/probe/missing")).expectError(404, "COMMON-005")
	}

	@Test
	fun `H9 처리되지 않은 예외는 500 COMMON-006이고 내부 상세를 노출하지 않는다`() {
		val body = probe("unknown")
			.expectError(500, "COMMON-006", "서버 내부 오류가 발생했습니다.")
			.andReturn().response.contentAsString

		assertFalse(body.contains("secret-internal-detail"))
	}

	@Test
	fun `H10 에러 응답에는 details 키가 null로 존재한다`() {
		probe("duplicate")
			.expectError(409, "USER-001")
			.andExpect(jsonPath("$.error").value(hasKey("details")))
			.andExpect(jsonPath("$.error.details").value(nullValue()))
	}

	@Test
	fun `H11 애플리케이션 예외는 ErrorCode의 코드와 문장으로 응답하고 details를 싣는다`() {
		probe("detailed")
			.expectError(409, "USER-001", "이미 가입된 이메일입니다.")
			.andExpect(jsonPath("$.error.details.remainingAttempts").value(4))
	}

	@Test
	fun `H14 cause 없는 애플리케이션 예외는 코드·종류·요청·개발 메모를 INFO로 남긴다`(output: CapturedOutput) {
		probe("detailed")

		val line = output.out.lines().single { it.contains("[USER-001 DUPLICATE_EMAIL]") }
		assertTrue(line.contains("INFO"), line)
		assertTrue(line.contains("GET /probe/throw"), line)
		assertTrue(line.contains("내부-디버그-메모"), line)
		assertTrue(line.endsWith("details={remainingAttempts=4}"), line)
	}

	@Test
	fun `H15 개발 메모는 응답 본문에 나가지 않는다`() {
		val body = probe("detailed").andReturn().response.contentAsString

		assertFalse(body.contains("내부-디버그-메모"))
	}

	@Test
	fun `H16 cause 있는 애플리케이션 예외는 WARN과 원인 스택 트레이스를 남긴다`(output: CapturedOutput) {
		probe("caused")

		val lines = output.out.lines()
		val line = lines.single { it.contains("[USER-001 DUPLICATE_EMAIL]") }
		assertTrue(line.contains("WARN"), line)
		assertTrue(line.endsWith("details=null"), line) // 로그 형식이 그대로다 (자리표시자를 늘리면 `{}`가 글자로 남아 실패)
		assertTrue(lines.any { it == "java.lang.IllegalStateException: 원인-예외" }, output.out)
		assertTrue(lines.any { it.startsWith("	at ") }, output.out)
	}
}
