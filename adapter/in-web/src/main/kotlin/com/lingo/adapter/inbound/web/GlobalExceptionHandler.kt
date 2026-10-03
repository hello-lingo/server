package com.lingo.adapter.inbound.web

import com.lingo.application.user.DuplicateEmailException
import com.lingo.domain.user.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateEmailException::class)
	fun handleDuplicateEmail(e: DuplicateEmailException): ResponseEntity<ErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("DUPLICATE_EMAIL", "이미 가입된 이메일입니다."))

	@ExceptionHandler(DomainException::class)
	fun handleDomain(e: DomainException): ResponseEntity<ErrorResponse> =
		invalidRequest(e.message ?: INVALID_MESSAGE)

	@ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
	fun handleMalformed(): ResponseEntity<ErrorResponse> = invalidRequest(INVALID_MESSAGE)

	private fun invalidRequest(message: String): ResponseEntity<ErrorResponse> =
		ResponseEntity.badRequest().body(ErrorResponse("INVALID_REQUEST", message))

	private companion object {
		const val INVALID_MESSAGE = "요청 값이 올바르지 않습니다."
	}
}
