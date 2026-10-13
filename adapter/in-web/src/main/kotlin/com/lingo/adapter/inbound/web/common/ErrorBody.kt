package com.lingo.adapter.inbound.web.common

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.ALWAYS)
data class ErrorBody(
	val code: String,
	val message: String,
	val details: Map<String, Any?>? = null,
)
