package com.lingo.adapter.inbound.web.common

import com.lingo.application.user.DuplicateEmailException
import com.lingo.domain.user.DomainException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(DuplicateEmailException::class)
	fun handleDuplicateEmail(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.DUPLICATE_EMAIL)

	@ExceptionHandler(DomainException::class)
	fun handleDomain(e: DomainException): ResponseEntity<ApiResponse<Nothing>> =
		respond(ErrorCode.INVALID_REQUEST, e.message ?: ErrorCode.INVALID_REQUEST.message)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(): ResponseEntity<ApiResponse<Nothing>> = respond(ErrorCode.VALIDATION_FAILED)

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

	private fun respond(errorCode: ErrorCode, message: String = errorCode.message): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(errorCode.status).body(ApiResponse.failure(errorCode, message))
}
