package com.lingo.adapter.inbound.web.school.response

import com.lingo.application.school.usecase.AdminSchoolResult

data class AdminSchoolResponse(
	val id: Long,
	val name: String,
	val hidden: Boolean,
	val domains: List<AdminSchoolDomainResponse>,
) {
	companion object {
		fun from(result: AdminSchoolResult) = AdminSchoolResponse(
			id = result.id,
			name = result.name,
			hidden = result.hidden,
			domains = result.domains.map { AdminSchoolDomainResponse(id = it.id, domain = it.domain) },
		)
	}
}

data class AdminSchoolDomainResponse(
	val id: Long,
	val domain: String,
)
