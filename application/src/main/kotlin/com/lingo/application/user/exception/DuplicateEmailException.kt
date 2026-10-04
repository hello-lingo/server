package com.lingo.application.user.exception

class DuplicateEmailException(
	message: String = "이미 가입된 이메일입니다.",
	cause: Throwable? = null,
) : RuntimeException(message, cause)
