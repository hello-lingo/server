package com.lingo.config

import com.lingo.adapter.inbound.web.common.ApiResponse
import com.lingo.adapter.inbound.web.common.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper

class JsonAccessDeniedHandler(
	private val jsonMapper: JsonMapper,
) : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		response.status = ErrorCode.FORBIDDEN.status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.characterEncoding = Charsets.UTF_8.name()
		response.writer.write(jsonMapper.writeValueAsString(ApiResponse.failure(ErrorCode.FORBIDDEN)))
	}
}
