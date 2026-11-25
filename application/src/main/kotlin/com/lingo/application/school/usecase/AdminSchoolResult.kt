package com.lingo.application.school.usecase

import com.lingo.domain.school.School

/** 관리자 화면용 학교 한 개 (숨김 여부와 도메인 id 포함) */
data class AdminSchoolResult(
	val id: Long,
	val name: String,
	val hidden: Boolean,
	val domains: List<AdminSchoolDomainResult>,
) {
	companion object {
		fun from(school: School) = AdminSchoolResult(
			id = requireNotNull(school.id) { "저장된 학교에 id가 없습니다." },
			name = school.name.value,
			hidden = school.hidden,
			domains = school.domains.map {
				AdminSchoolDomainResult(id = requireNotNull(it.id) { "저장된 도메인에 id가 없습니다." }, domain = it.domain.value)
			},
		)
	}
}

data class AdminSchoolDomainResult(
	val id: Long,
	val domain: String,
)
