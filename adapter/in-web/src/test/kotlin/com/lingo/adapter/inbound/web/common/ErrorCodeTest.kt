package com.lingo.adapter.inbound.web.common

import com.lingo.application.common.error.UserErrorType
import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorCodeTest {

	@Test
	fun `EC1 code 값은 모두 유일하다`() {
		val codes = ErrorCode.entries.map { it.code }

		assertEquals(codes.size, codes.toSet().size)
	}

	@Test
	fun `EC2 code는 도메인 접두 형식이고 메시지는 비어있지 않다`() {
		val format = Regex("^(USER|AUTH|COMMON)-\\d{3}$")

		ErrorCode.entries.forEach {
			assertTrue(format.matches(it.code), it.code)
			assertTrue(it.message.isNotBlank(), it.name)
		}
	}

	@Test
	fun `EC3 상수별 code와 status가 표와 일치한다`() {
		val expected = mapOf(
			ErrorCode.DUPLICATE_EMAIL to ("USER-001" to HttpStatus.CONFLICT),
			ErrorCode.INVALID_CREDENTIALS to ("AUTH-001" to HttpStatus.UNAUTHORIZED),
			ErrorCode.UNAUTHORIZED to ("AUTH-002" to HttpStatus.UNAUTHORIZED),
			ErrorCode.FORBIDDEN to ("AUTH-003" to HttpStatus.FORBIDDEN),
			ErrorCode.VALIDATION_FAILED to ("COMMON-001" to HttpStatus.BAD_REQUEST),
			ErrorCode.INVALID_REQUEST to ("COMMON-002" to HttpStatus.BAD_REQUEST),
			ErrorCode.METHOD_NOT_ALLOWED to ("COMMON-003" to HttpStatus.METHOD_NOT_ALLOWED),
			ErrorCode.UNSUPPORTED_MEDIA_TYPE to ("COMMON-004" to HttpStatus.UNSUPPORTED_MEDIA_TYPE),
			ErrorCode.NOT_FOUND to ("COMMON-005" to HttpStatus.NOT_FOUND),
			ErrorCode.INTERNAL_ERROR to ("COMMON-006" to HttpStatus.INTERNAL_SERVER_ERROR),
			ErrorCode.FILE_TOO_LARGE to ("COMMON-007" to HttpStatus.CONTENT_TOO_LARGE),
		)

		assertEquals(expected.keys, ErrorCode.entries.toSet())
		expected.forEach { (errorCode, spec) ->
			assertEquals(spec.first, errorCode.code, errorCode.name)
			assertEquals(spec.second, errorCode.status, errorCode.name)
		}
	}

	@Test
	fun `EC4 ErrorType은 대응하는 ErrorCode로 연결되고 UserErrorType 전부가 연결된다`() {
		assertEquals(ErrorCode.DUPLICATE_EMAIL, ErrorCode.from(UserErrorType.DUPLICATE_EMAIL))
		assertEquals(ErrorCode.INVALID_CREDENTIALS, ErrorCode.from(UserErrorType.INVALID_CREDENTIALS))
		UserErrorType.entries.forEach { ErrorCode.from(it) }
	}
}
