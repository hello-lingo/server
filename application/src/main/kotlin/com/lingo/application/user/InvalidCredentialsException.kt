package com.lingo.application.user

class InvalidCredentialsException(
	message: String = "이메일 또는 비밀번호가 올바르지 않습니다.",
	cause: Throwable? = null,
) : RuntimeException(message, cause)
