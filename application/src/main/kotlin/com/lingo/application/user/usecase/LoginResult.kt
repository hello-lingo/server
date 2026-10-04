package com.lingo.application.user.usecase

data class LoginResult(val accessToken: String, val expiresIn: Long) {
	override fun toString(): String = "LoginResult(accessToken=****, expiresIn=$expiresIn)"
}
