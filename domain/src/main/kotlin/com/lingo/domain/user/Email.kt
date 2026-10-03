package com.lingo.domain.user

@JvmInline
value class Email private constructor(val value: String) {

	companion object {
		private const val MAX_LENGTH = 254
		private val FORMAT = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

		fun of(raw: String): Email {
			val normalized = raw.trim().lowercase()
			if (normalized.length > MAX_LENGTH || !FORMAT.matches(normalized)) {
				throw InvalidEmailException("이메일 형식이 올바르지 않습니다.")
			}
			return Email(normalized)
		}
	}
}
