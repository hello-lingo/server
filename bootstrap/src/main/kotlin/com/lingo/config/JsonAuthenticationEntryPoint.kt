package com.lingo.config

import com.lingo.adapter.inbound.web.common.ApiResponse
import com.lingo.adapter.inbound.web.common.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.json.JsonMapper

class JsonAuthenticationEntryPoint(
	private val jsonMapper: JsonMapper,
) : AuthenticationEntryPoint {

	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		response.status = ErrorCode.UNAUTHORIZED.status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.characterEncoding = Charsets.UTF_8.name()
		response.writer.write(jsonMapper.writeValueAsString(ApiResponse.failure(ErrorCode.UNAUTHORIZED)))
	}
}
