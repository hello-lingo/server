package com.lingo.application.user

data class SignUpCommand(
	val email: String,
	val password: String,
	val name: String,
) {
	override fun toString(): String = "SignUpCommand(email=$email, password=****, name=$name)"
}
