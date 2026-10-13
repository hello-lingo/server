package com.lingo.domain.sample

import com.lingo.domain.common.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExtensibleDomainExceptionTest {

	@Test
	fun `DE3 다른 패키지에서 DomainException을 상속할 수 있고 메시지가 보존된다`() {
		val e: Throwable = SampleDomainException()

		assertIs<DomainException>(e)
		assertEquals("샘플", e.message)
	}
}

private class SampleDomainException : DomainException("샘플")
