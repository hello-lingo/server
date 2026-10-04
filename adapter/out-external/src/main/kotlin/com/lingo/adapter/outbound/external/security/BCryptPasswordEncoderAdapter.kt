package com.lingo.adapter.outbound.external.security

import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.domain.user.RawPassword
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordEncoderAdapter : PasswordEncoderPort {

	private val encoder = BCryptPasswordEncoder()

	override fun encode(raw: RawPassword): String = encoder.encode(raw.value)!!
}
