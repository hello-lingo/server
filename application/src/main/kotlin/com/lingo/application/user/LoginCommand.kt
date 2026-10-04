package com.lingo.application.user

data class LoginCommand(
	val email: String,
	val password: String,
) {
	override fun toString(): String = "LoginCommand(email=$email, password=****)"
}
