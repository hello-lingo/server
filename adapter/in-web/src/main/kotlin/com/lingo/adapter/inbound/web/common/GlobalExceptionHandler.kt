package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.ApplicationException
import com.lingo.domain.common.DomainException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.method.ParameterErrors
import org.springframework.web.ErrorResponse
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ApplicationException::class)
	fun handleApplication(e: ApplicationException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		val errorCode = ErrorCode.from(e.errorType)
		val format = "[{} {}] {} {} - {} details={}"
		val args = arrayOf(errorCode.code, e.errorType, request.method, request.requestURI, e.debugMessage ?: "-", e.details)
		if (e.cause == null) {
			log.info(format, *args)
		} else {
			// 마지막 인자가 Throwable이면 자리표시자 수와 관계없이 스택 트레이스로 출력된다.
			// cause는 반드시 마지막 인자로 그대로 넘긴다. 문자열로 바꾸면 스택 트레이스가 사라진다 (H16이 고정)
			log.warn(format, *args, e.cause)
		}
		return respond(errorCode, errorCode.message, e.details)
	}

	@ExceptionHandler(DomainException::class)
	fun handleDomain(e: DomainException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		val message = e.message ?: ErrorCode.INVALID_REQUEST.message
		logClientError(ErrorCode.INVALID_REQUEST, request, message)
		return respond(ErrorCode.INVALID_REQUEST, message)
	}

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(e: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respondValidationFailed(request, fieldErrorsOf(e.bindingResult))

	// @RequestParam·@PathVariable 등에 붙인 제약 위반 (컨트롤러에 @Validated가 없을 때 Spring MVC 내장 메서드 검증). field = 파라미터 이름.
	// 같은 메서드에 @Valid 본문이 있으면 본문 오류도 여기로 온다 → 그 오류는 본문 필드 이름으로 (MethodArgumentNotValidException과 같은 계약)
	@ExceptionHandler(HandlerMethodValidationException::class)
	fun handleMethodValidation(e: HandlerMethodValidationException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		val items = e.parameterValidationResults.flatMap { result ->
			if (result is ParameterErrors) {
				fieldErrorItemsOf(result)
			} else {
				result.resolvableErrors.map { FieldErrorItem(result.methodParameter.parameterName, it.defaultMessage ?: "") }
			}
		}
		return respondValidationFailed(request, fieldErrorsOf(items))
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleNotReadable(e: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		// 원인 예외의 메시지에는 입력값이 들어 있을 수 있으므로 클래스 이름만 남긴다
		logClientError(ErrorCode.INVALID_REQUEST, request, e.mostSpecificCause.javaClass.simpleName)
		return respond(ErrorCode.INVALID_REQUEST)
	}

	@ExceptionHandler(
		MethodArgumentTypeMismatchException::class,
		MissingServletRequestParameterException::class,
		MissingRequestHeaderException::class,
		MissingServletRequestPartException::class,
	)
	fun handleBadRequest(e: Exception, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respondClientError(ErrorCode.INVALID_REQUEST, e, request)

	@ExceptionHandler(HttpRequestMethodNotSupportedException::class)
	fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respondClientError(ErrorCode.METHOD_NOT_ALLOWED, e, request)

	@ExceptionHandler(HttpMediaTypeNotSupportedException::class)
	fun handleMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respondClientError(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e, request)

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNoResource(e: NoResourceFoundException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respondClientError(ErrorCode.NOT_FOUND, e, request)

	// 파일 하나가 spring.servlet.multipart.max-file-size(또는 요청 전체가 max-request-size)를 넘었을 때
	@ExceptionHandler(MaxUploadSizeExceededException::class)
	fun handleMaxUploadSize(e: MaxUploadSizeExceededException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		logClientError(ErrorCode.FILE_TOO_LARGE, request, "MaxUploadSizeExceededException")
		return respond(ErrorCode.FILE_TOO_LARGE, details = mapOf("maxSizeMb" to UploadLimits.MAX_FILE_SIZE_MB))
	}

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(e: Exception, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
		// 안전장치: 처리 메서드가 없어도 Spring이 4xx로 정의한 예외는 사용자 실수이므로 500·ERROR로 만들지 않는다
		if (e is ErrorResponse && e.statusCode.is4xxClientError) {
			return respondClientError(ErrorCode.INVALID_REQUEST, e, request, e.statusCode)
		}
		log.error("처리되지 않은 예외", e)
		return respond(ErrorCode.INTERNAL_ERROR)
	}

	// 처리된 4xx 한 줄 기록. 요약에 입력값을 넣지 않는다.
	private fun logClientError(errorCode: ErrorCode, request: HttpServletRequest, summary: String) =
		log.info("[{}] {} {} - {}", errorCode.code, request.method, request.requestURI, summary)

	// 요약이 예외 클래스 이름인 4xx: 로그 + 응답
	private fun respondClientError(
		errorCode: ErrorCode,
		e: Exception,
		request: HttpServletRequest,
		status: HttpStatusCode = errorCode.status,
	): ResponseEntity<ApiResponse<Nothing>> {
		logClientError(errorCode, request, e.javaClass.simpleName)
		return respond(errorCode, status = status)
	}

	private fun respondValidationFailed(request: HttpServletRequest, details: Map<String, List<Map<String, String?>>>): ResponseEntity<ApiResponse<Nothing>> {
		logClientError(ErrorCode.VALIDATION_FAILED, request, "fieldErrors=${details.getValue("fieldErrors").map { it["field"] }}")
		return respond(ErrorCode.VALIDATION_FAILED, details = details)
	}

	// Content-Type을 JSON으로 고정한다.
	// 정하지 않으면 Accept에 JSON이 없을 때(예: 406) 본문을 쓰지 못하고,
	// DefaultHandlerExceptionResolver로 넘어가 빈 본문 + 입력값이 담긴 WARN 로그가 남는다.
	private fun respond(
		errorCode: ErrorCode,
		message: String = errorCode.message,
		details: Map<String, Any?>? = null,
		status: HttpStatusCode = errorCode.status,
	): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(errorCode, message, details))

	// 특정 필드에 속하지 않는 오류는 field = null, null 먼저 → field → reason 순. 입력값(rejectedValue)은 넣지 않는다
	private fun fieldErrorsOf(result: BindingResult): Map<String, List<Map<String, String?>>> = fieldErrorsOf(fieldErrorItemsOf(result))

	private fun fieldErrorItemsOf(errors: Errors): List<FieldErrorItem> {
		val global = errors.globalErrors.map { FieldErrorItem(null, it.defaultMessage ?: "") }
		val fields = errors.fieldErrors.map { FieldErrorItem(it.field, it.defaultMessage ?: "") }
		return global + fields
	}

	private fun fieldErrorsOf(items: List<FieldErrorItem>): Map<String, List<Map<String, String?>>> {
		val sorted = items.sortedWith(compareBy(nullsFirst()) { it: FieldErrorItem -> it.field }.thenBy { it.reason })
		return mapOf("fieldErrors" to sorted.map { mapOf("field" to it.field, "reason" to it.reason) })
	}

	private data class FieldErrorItem(val field: String?, val reason: String)
}
