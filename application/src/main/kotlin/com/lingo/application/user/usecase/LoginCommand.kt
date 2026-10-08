package com.lingo.application.user.usecase

data class LoginCommand(
	val email: String,
	val password: String,
) {
	override fun toString(): String = "LoginCommand(email=$email, password=****)"
}
