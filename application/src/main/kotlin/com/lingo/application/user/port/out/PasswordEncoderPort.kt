package com.lingo.application.user.port.out

import com.lingo.domain.user.RawPassword

interface PasswordEncoderPort {
	fun encode(raw: RawPassword): String

	fun matches(raw: String, hash: String): Boolean
}
