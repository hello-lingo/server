package com.lingo.adapter.inbound.web.user.request

import com.lingo.application.user.LoginCommand
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
	@field:NotBlank val email: String,
	@field:NotBlank val password: String,
) {
	fun toCommand() = LoginCommand(email = email, password = password)

	override fun toString(): String = "LoginRequest(email=$email, password=****)"
}
