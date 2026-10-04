package com.lingo.application.user

data class IssuedAccessToken(val token: String, val expiresIn: Long) {
	override fun toString(): String = "IssuedAccessToken(token=****, expiresIn=$expiresIn)"
}
