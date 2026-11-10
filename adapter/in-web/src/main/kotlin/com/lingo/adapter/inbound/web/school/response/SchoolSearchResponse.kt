package com.lingo.adapter.inbound.web.school.response

import com.lingo.application.school.usecase.SchoolSearchResult

/**
 * 드롭다운 한 줄
 * 고르면 schoolName과 domain이 함께 채워진다 */
data class SchoolSearchResponse(
	val schoolId: Long,
	val schoolName: String,
	val domainId: Long,
	val domain: String,
) {
	companion object {
		fun from(result: SchoolSearchResult) = SchoolSearchResponse(
			schoolId = result.schoolId,
			schoolName = result.schoolName,
			domainId = result.domainId,
			domain = result.domain,
		)
	}
}
