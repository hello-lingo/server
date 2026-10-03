package com.lingo.domain.user

class RawPassword private constructor(val value: String) {

	override fun toString(): String = "RawPassword(****)"

	companion object {
		private const val MIN_LENGTH = 8
		private const val MAX_LENGTH = 64
		private val ALLOWED = Regex("^[\\x21-\\x7E]+$")
		private val LETTER = Regex("[A-Za-z]")
		private val DIGIT = Regex("[0-9]")
		private val SPECIAL = Regex("[^A-Za-z0-9]")

		fun of(raw: String): RawPassword {
			val valid = raw.length in MIN_LENGTH..MAX_LENGTH &&
				ALLOWED.matches(raw) &&
				LETTER.containsMatchIn(raw) &&
				DIGIT.containsMatchIn(raw) &&
				SPECIAL.containsMatchIn(raw)
			if (!valid) {
				throw InvalidPasswordException("비밀번호는 8~64자의 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
			}
			return RawPassword(raw)
		}
	}
}
