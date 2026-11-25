package com.lingo.domain.school

/**
 * 학교 이메일의 '@' 뒤 부분 (예: cau.ac.kr)
 * 앞뒤 공백 제거 + 소문자로 정규화
 * */
@JvmInline
value class SchoolDomain private constructor(val value: String) {

	companion object {
		private const val MAX_LENGTH = 253
		// 영문 소문자·숫자·하이픈으로 된 라벨이 점으로 2개 이상 이어진 형태. 라벨은 하이픈으로 시작·끝나지 않는다
		private val FORMAT = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")

		fun of(raw: String): SchoolDomain {
			val normalized = raw.trim().lowercase()
			if (normalized.length > MAX_LENGTH || !FORMAT.matches(normalized)) {
				throw InvalidSchoolDomainException("학교 도메인 형식이 올바르지 않습니다.")
			}
			return SchoolDomain(normalized)
		}
	}
}
