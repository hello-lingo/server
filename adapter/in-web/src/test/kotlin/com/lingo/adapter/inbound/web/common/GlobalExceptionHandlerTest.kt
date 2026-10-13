package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.UserErrorType
import com.lingo.application.user.exception.DuplicateEmailException
import com.lingo.application.user.exception.InvalidCredentialsException
import com.lingo.domain.user.InvalidEmailException
import com.lingo.domain.user.InvalidNameException
import com.lingo.domain.user.InvalidPasswordException
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.hamcrest.Matchers.aMapWithSize
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.nullValue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class ProbeBody(@field:NotBlank(message = "필수 입력 항목입니다.") val name: String)

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
		"global" -> invalidBody(
			FieldError("probeBody", "zeta", "필드 오류"),
			FieldError("probeBody", "name", "필드 오류"),
			ObjectError("probeBody", "전체 오류"),
		)
		"multi" -> invalidBody(FieldError("probeBody", "name", "나 메시지"), FieldError("probeBody", "name", "가 메시지"))
		"part" -> MissingServletRequestPartException("file")
		"notAcceptable" -> HttpMediaTypeNotAcceptableException(listOf(MediaType.APPLICATION_JSON)) // 406, 처리 메서드 없음 → 안전장치
		else -> IllegalStateException("secret-internal-detail")
	}

	// MethodArgumentNotValidException을 직접 만들어 던진다 (필드에 속하지 않는 오류·같은 필드의 여러 오류)
	private fun invalidBody(vararg errors: ObjectError): MethodArgumentNotValidException {
		val parameter = MethodParameter(ExceptionProbeController::class.java.getMethod("body", ProbeBody::class.java), 0)
		val bindingResult = BeanPropertyBindingResult(ProbeBody("x"), "probeBody").apply { errors.forEach(::addError) }
		return MethodArgumentNotValidException(parameter, bindingResult)
	}

	@PostMapping("/probe/body", consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun body(@Valid @RequestBody body: ProbeBody): String = body.name

	@GetMapping("/probe/typed/{id}")
	fun typed(@PathVariable id: Long): Long = id

	@GetMapping("/probe/param")
	fun param(@RequestParam("q") q: String): String = q

	@GetMapping("/probe/header")
	fun header(@RequestHeader("X-Probe") h: String): String = h

	// 클래스에 @Validated가 없어야 Spring MVC 내장 메서드 검증이 HandlerMethodValidationException을 던진다
	// (@Validated가 있으면 AOP 검증이 먼저 돌아 ConstraintViolationException → 500)
	@GetMapping("/probe/min")
	fun min(@RequestParam("n") @Min(value = 1, message = "1 이상이어야 합니다.") n: Int): Int = n

	// 파라미터 제약과 @Valid 본문이 함께 있으면 본문 오류도 HandlerMethodValidationException으로 합쳐진다
	@PostMapping("/probe/combo/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun combo(
		@PathVariable @Min(value = 1, message = "1 이상이어야 합니다.") id: Long,
		@Valid @RequestBody body: ProbeBody,
	): String = body.name

	@GetMapping("/probe/json", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun json(): String = "{}"
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
	fun `H12 Bean Validation 실패는 details에 필드별 오류를 싣는다`() {
		mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))
			.expectError(400, "COMMON-001", "요청 값이 올바르지 않습니다.")
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(1))
			.andExpect(jsonPath("$.error.details.fieldErrors[0]").value(aMapWithSize<String, Any?>(2))) // field·reason만, 입력값 없음
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("name"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("필수 입력 항목입니다."))
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

	@Test
	fun `H25 특정 필드에 속하지 않는 오류는 field가 null로 맨 앞에 오고 나머지는 필드 이름순이다`() {
		probe("global")
			.expectError(400, "COMMON-001", "요청 값이 올바르지 않습니다.")
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(3))
			.andExpect(jsonPath("$.error.details.fieldErrors[0]").value(hasKey("field")))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value(nullValue()))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("전체 오류"))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].field").value("name"))
			.andExpect(jsonPath("$.error.details.fieldErrors[2].field").value("zeta"))
	}

	@Test
	fun `H26 같은 필드의 오류는 reason 오름차순으로 정렬한다`() {
		probe("multi")
			.expectError(400, "COMMON-001")
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(2))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("name"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("가 메시지"))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].field").value("name"))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].reason").value("나 메시지"))
	}

	@Test
	fun `H17 도메인 예외는 코드·요청·예외 메시지를 INFO로 남긴다`(output: CapturedOutput) {
		probe("email")

		val line = output.out.lines().single { it.contains("[COMMON-002] GET /probe/throw - 이메일 위반") }
		assertTrue(line.contains("INFO"), line)
	}

	@Test
	fun `H18 검증 실패는 필드 이름만 INFO로 남긴다`(output: CapturedOutput) {
		mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))

		val line = output.out.lines().single { it.contains("[COMMON-001] POST /probe/body - fieldErrors=[name]") }
		assertTrue(line.contains("INFO"), line)
	}

	@Test
	fun `H19 경로 변수 타입이 맞지 않으면 400 COMMON-002`() {
		mockMvc.perform(get("/probe/typed/abc")).expectError(400, "COMMON-002", "잘못된 요청입니다.")
	}

	@Test
	fun `H20 필수 요청 파라미터가 없으면 400 COMMON-002`() {
		mockMvc.perform(get("/probe/param")).expectError(400, "COMMON-002", "잘못된 요청입니다.")
	}

	@Test
	fun `H21 메서드 파라미터 검증 실패는 400 COMMON-001이고 field는 파라미터 이름, 본문 인자 오류는 본문 필드 이름이다`() {
		mockMvc.perform(get("/probe/min").param("n", "0"))
			.expectError(400, "COMMON-001", "요청 값이 올바르지 않습니다.")
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(1))
			.andExpect(jsonPath("$.error.details.fieldErrors[0]").value(aMapWithSize<String, Any?>(2)))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("n"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("1 이상이어야 합니다."))

		// @Valid 본문 인자의 오류는 파라미터 이름(body)이 아니라 본문 필드 이름으로 (MethodArgumentNotValidException과 같은 계약)
		mockMvc.perform(post("/probe/combo/0").contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))
			.expectError(400, "COMMON-001")
			.andExpect(jsonPath("$.error.details.fieldErrors.length()").value(2))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("id"))
			.andExpect(jsonPath("$.error.details.fieldErrors[0].reason").value("1 이상이어야 합니다."))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].field").value("name"))
			.andExpect(jsonPath("$.error.details.fieldErrors[1].reason").value("필수 입력 항목입니다."))
	}

	@Test
	fun `H22 필수 요청 헤더가 없으면 400 COMMON-002`() {
		mockMvc.perform(get("/probe/header")).expectError(400, "COMMON-002", "잘못된 요청입니다.")
	}

	@Test
	fun `H23 필수 multipart 파트가 없으면 400 COMMON-002`() {
		probe("part").expectError(400, "COMMON-002", "잘못된 요청입니다.")
	}

	@Test
	fun `H24 처리 메서드가 없는 Spring 4xx 예외는 그 상태 코드와 COMMON-002로 응답하고 INFO로 남긴다`(output: CapturedOutput) {
		probe("notAcceptable").expectError(406, "COMMON-002", "잘못된 요청입니다.")

		// 실제 협상 실패: Accept에 JSON이 없어도 에러 본문은 JSON으로 나간다
		mockMvc.perform(get("/probe/json").accept(MediaType.TEXT_PLAIN))
			.expectError(406, "COMMON-002", "잘못된 요청입니다.")

		listOf("/probe/throw", "/probe/json").forEach { uri ->
			val line = output.out.lines().single { it.contains("[COMMON-002] GET $uri - HttpMediaTypeNotAcceptableException") }
			assertTrue(line.contains("INFO"), line)
		}
		assertFalse(output.out.contains("처리되지 않은 예외"), output.out)
		assertFalse(output.out.contains("Failure in @ExceptionHandler"), output.out)
	}
}
