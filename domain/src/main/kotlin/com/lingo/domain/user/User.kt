package com.lingo.domain.user

data class User(
	val id: Long?,
	val email: Email,
	val passwordHash: String,
	val name: String,
) {

	companion object {
		private const val MAX_NAME_LENGTH = 50

		fun signUp(email: Email, passwordHash: String, name: String): User =
			User(id = null, email = email, passwordHash = passwordHash, name = validateName(name))

		/** 앞뒤 공백을 제거한 이름을 반환하고, 1~50자가 아니면 [InvalidNameException]을 던진다. */
		fun validateName(name: String): String {
			val trimmed = name.trim()
			if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) {
				throw InvalidNameException("이름은 1~50자여야 합니다.")
			}
			return trimmed
		}
	}
}
