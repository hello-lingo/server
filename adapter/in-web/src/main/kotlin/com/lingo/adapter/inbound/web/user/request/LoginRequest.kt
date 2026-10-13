package com.lingo.adapter.inbound.web.user.request

import com.lingo.application.user.usecase.LoginCommand
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
	@field:NotBlank(message = "필수 입력 항목입니다.") val email: String,
	@field:NotBlank(message = "필수 입력 항목입니다.") val password: String,
) {
	fun toCommand() = LoginCommand(email = email, password = password)

	override fun toString(): String = "LoginRequest(email=$email, password=****)"
}
