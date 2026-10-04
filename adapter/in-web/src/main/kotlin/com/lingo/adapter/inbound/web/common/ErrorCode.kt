package com.lingo.adapter.inbound.web.common

import org.springframework.http.HttpStatus

// 코드 접두는 도메인 규칙을 따른다: USER-, AUTH-, COMMON-
enum class ErrorCode(val code: String, val message: String, val status: HttpStatus) {
	DUPLICATE_EMAIL("USER-001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
	VALIDATION_FAILED("COMMON-001", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	INVALID_REQUEST("COMMON-002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
	METHOD_NOT_ALLOWED("COMMON-003", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
	UNSUPPORTED_MEDIA_TYPE("COMMON-004", "지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	NOT_FOUND("COMMON-005", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INTERNAL_ERROR("COMMON-006", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
}
