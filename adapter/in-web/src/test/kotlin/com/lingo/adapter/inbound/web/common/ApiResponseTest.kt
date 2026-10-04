package com.lingo.adapter.inbound.web.common

import com.lingo.adapter.inbound.web.user.SignUpResponse
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiResponseTest {

	private val mapper = jacksonObjectMapper()

	private fun json(value: Any): JsonNode = mapper.readTree(mapper.writeValueAsString(value))

	@Test
	fun `AR1 성공 응답은 data를 담고 error 키는 null로 존재한다`() {
		val node = json(ApiResponse.success(SignUpResponse(1L, "a@b.com")))

		assertTrue(node["success"].asBoolean())
		assertEquals(1L, node["data"]["userId"].asLong())
		assertEquals("a@b.com", node["data"]["email"].asString())
		assertTrue(node.has("error"))
		assertTrue(node["error"].isNull)
	}

	@Test
	fun `AR2 실패 응답은 data 키가 null이고 error에 코드와 기본 메시지가 있다`() {
		val node = json(ApiResponse.failure(ErrorCode.DUPLICATE_EMAIL))

		assertEquals(false, node["success"].asBoolean())
		assertTrue(node.has("data"))
		assertTrue(node["data"].isNull)
		assertEquals("USER-001", node["error"]["code"].asString())
		assertEquals("이미 가입된 이메일입니다.", node["error"]["message"].asString())
	}

	@Test
	fun `AR3 실패 응답은 메시지 오버라이드를 반영한다`() {
		val node = json(ApiResponse.failure(ErrorCode.INVALID_REQUEST, "커스텀"))

		assertEquals("COMMON-002", node["error"]["code"].asString())
		assertEquals("커스텀", node["error"]["message"].asString())
	}
}
