package com.lingo.adapter.inbound.web.user.request

import com.lingo.application.user.usecase.SignUpCommand
import jakarta.validation.constraints.NotBlank

data class SignUpRequest(
	@field:NotBlank(message = "필수 입력 항목입니다.") val email: String,
	@field:NotBlank(message = "필수 입력 항목입니다.") val password: String,
	@field:NotBlank(message = "필수 입력 항목입니다.") val name: String,
) {
	fun toCommand() = SignUpCommand(email = email, password = password, name = name)

	override fun toString(): String = "SignUpRequest(email=$email, password=****, name=$name)"
}
