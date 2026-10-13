package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.ErrorType
import com.lingo.application.common.error.UserErrorType
import org.springframework.http.HttpStatus

// 코드 접두는 도메인 규칙을 따른다: USER-, AUTH-, COMMON-
/** details를 쓰는 항목은 그 모양을 KDoc으로 적는다 */
enum class ErrorCode(val code: String, val message: String, val status: HttpStatus) {
	DUPLICATE_EMAIL("USER-001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
	INVALID_CREDENTIALS("AUTH-001", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	UNAUTHORIZED("AUTH-002", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("AUTH-003", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
	/** details: { fieldErrors: [{ field: String?, reason: String }] } */
	VALIDATION_FAILED("COMMON-001", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	INVALID_REQUEST("COMMON-002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
	METHOD_NOT_ALLOWED("COMMON-003", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
	UNSUPPORTED_MEDIA_TYPE("COMMON-004", "지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	NOT_FOUND("COMMON-005", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	INTERNAL_ERROR("COMMON-006", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	;

	companion object {
		// else를 두지 않아 새 종류를 빠뜨리면 컴파일 오류
		fun from(errorType: ErrorType): ErrorCode = when (errorType) {
			is UserErrorType -> when (errorType) {
				UserErrorType.DUPLICATE_EMAIL -> DUPLICATE_EMAIL
				UserErrorType.INVALID_CREDENTIALS -> INVALID_CREDENTIALS
			}
		}
	}
}
