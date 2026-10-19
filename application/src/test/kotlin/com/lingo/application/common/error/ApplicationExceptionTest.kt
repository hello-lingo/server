package com.lingo.application.common.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

// ErrorType은 sealed라 테스트에서 새로 만들 수 없으므로 기존 UserErrorType 값을 쓴다
private class PlainException(debugMessage: String? = null, cause: Throwable? = null) :
	ApplicationException(UserErrorType.DUPLICATE_EMAIL, debugMessage, cause)

private class DetailedException : ApplicationException(UserErrorType.DUPLICATE_EMAIL) {
	override val details = mapOf("remainingAttempts" to 4)
}

class ApplicationExceptionTest {

	@Test
	fun `AE1 개발 메모 없이 만들면 errorType과 cause를 보존하고 message는 errorType 이름이며 details는 null이다`() {
		val cause = IllegalStateException("원인")

		val exception = PlainException(cause = cause)

		assertEquals(UserErrorType.DUPLICATE_EMAIL, exception.errorType)
		assertSame(cause, exception.cause)
		assertNull(exception.debugMessage)
		assertEquals("DUPLICATE_EMAIL", exception.message)
		assertNull(exception.details)
	}

	@Test
	fun `AE2 하위 예외는 details를 재정의할 수 있다`() {
		val exception = DetailedException()

		assertEquals(mapOf("remainingAttempts" to 4), exception.details)
	}

	@Test
	fun `AE3 개발 메모를 주면 debugMessage와 message가 모두 그 메모다`() {
		val exception = PlainException(debugMessage = "동시 가입 의심")

		assertEquals("동시 가입 의심", exception.debugMessage)
		assertEquals("동시 가입 의심", exception.message)
	}
}
