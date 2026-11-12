package com.lingo.domain.school

/** 학교에 등록된 도메인 한 개. id는 저장 전이면 null (관리자 API가 도메인을 id로 가리킨다) */
data class RegisteredDomain(
	val id: Long?,
	val domain: SchoolDomain,
)
