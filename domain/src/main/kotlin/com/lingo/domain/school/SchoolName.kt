package com.lingo.domain.school

@JvmInline
value class SchoolName private constructor(val value: String) {

	companion object {
		private const val MAX_LENGTH = 100

		fun of(raw: String): SchoolName {
			val trimmed = raw.trim()
			if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) {
				throw InvalidSchoolNameException("학교 이름은 1~100자여야 합니다.")
			}
			return SchoolName(trimmed)
		}
	}
}
