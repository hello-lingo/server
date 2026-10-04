package com.lingo.adapter.inbound.web.user.response

data class LoginResponse(
	val accessToken: String,
	val expiresIn: Long,
	val tokenType: String = "Bearer",
) {
	override fun toString(): String = "LoginResponse(accessToken=****, tokenType=$tokenType, expiresIn=$expiresIn)"
}
