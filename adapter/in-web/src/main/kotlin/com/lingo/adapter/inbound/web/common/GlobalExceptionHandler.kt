package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.ApplicationException
import com.lingo.domain.common.DomainException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
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
	fun handleDomain(e: DomainException): ResponseEntity<ApiResponse<Nothing>> =
		respond(ErrorCode.INVALID_REQUEST, e.message ?: ErrorCode.INVALID_REQUEST.message)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(e: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> =
		respond(ErrorCode.VALIDATION_FAILED, details = fieldErrorsOf(e.bindingResult))

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleNotReadable(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.INVALID_REQUEST)

	@ExceptionHandler(HttpRequestMethodNotSupportedException::class)
	fun handleMethodNotSupported(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.METHOD_NOT_ALLOWED)

	@ExceptionHandler(HttpMediaTypeNotSupportedException::class)
	fun handleMediaTypeNotSupported(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE)

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNoResource(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.NOT_FOUND)

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
		log.error("처리되지 않은 예외", e)
		return respond(ErrorCode.INTERNAL_ERROR)
	}

	private fun respond(
		errorCode: ErrorCode,
		message: String = errorCode.message,
		details: Map<String, Any?>? = null,
		status: HttpStatusCode = errorCode.status,
	): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(status).body(ApiResponse.failure(errorCode, message, details))

	// 특정 필드에 속하지 않는 오류는 field = null, null 먼저 → field → reason 순. 입력값(rejectedValue)은 넣지 않는다
	private fun fieldErrorsOf(result: BindingResult): Map<String, Any?> {
		val global = result.globalErrors.map { FieldErrorItem(null, it.defaultMessage ?: "") }
		val fields = result.fieldErrors.map { FieldErrorItem(it.field, it.defaultMessage ?: "") }
		val sorted = (global + fields).sortedWith(compareBy(nullsFirst()) { it: FieldErrorItem -> it.field }.thenBy { it.reason })
		return mapOf("fieldErrors" to sorted.map { mapOf("field" to it.field, "reason" to it.reason) })
	}

	private data class FieldErrorItem(val field: String?, val reason: String)
}
