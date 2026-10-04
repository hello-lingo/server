package com.lingo.adapter.inbound.web.user.request

import com.lingo.application.user.SignUpCommand
import jakarta.validation.constraints.NotBlank

data class SignUpRequest(
	@field:NotBlank val email: String,
	@field:NotBlank val password: String,
	@field:NotBlank val name: String,
) {
	fun toCommand() = SignUpCommand(email = email, password = password, name = name)

	override fun toString(): String = "SignUpRequest(email=$email, password=****, name=$name)"
}
