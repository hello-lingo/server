package com.lingo.application.user.model

data class IssuedAccessJwt(val token: String, val expiresIn: Long) {
	override fun toString(): String = "IssuedAccessJwt(token=****, expiresIn=$expiresIn)"
}
