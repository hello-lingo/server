package com.lingo.application.school.usecase

/** 검색 결과 한 줄 = 학교 하나와 그 학교의 도메인 하나 */
data class SchoolSearchResult(
	val schoolId: Long,
	val schoolName: String,
	val domainId: Long,
	val domain: String,
)
