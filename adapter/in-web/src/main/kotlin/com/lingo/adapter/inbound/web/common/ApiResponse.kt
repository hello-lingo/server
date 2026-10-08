package com.lingo.adapter.inbound.web.common

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.ALWAYS)
@ConsistentCopyVisibility
data class ApiResponse<T> private constructor(
	val success: Boolean,
	val data: T?,
	val error: ErrorBody?,
) {
	companion object {
		fun <T> success(data: T): ApiResponse<T> = ApiResponse(true, data, null)

		fun failure(errorCode: ErrorCode, message: String = errorCode.message): ApiResponse<Nothing> =
			ApiResponse(false, null, ErrorBody(errorCode.code, message))
	}
}
