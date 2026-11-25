package com.lingo.adapter.inbound.web.school

import com.lingo.adapter.inbound.web.common.ApiResponse
import com.lingo.adapter.inbound.web.school.response.SchoolSearchResponse
import com.lingo.application.school.usecase.SchoolSearchType
import com.lingo.application.school.usecase.SearchSchoolsQuery
import com.lingo.application.school.usecase.SearchSchoolsService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/schools")
class SchoolController(
	private val searchSchoolsService: SearchSchoolsService,
) {

	/** 학교 이름(type=NAME) 또는 도메인(type=DOMAIN)으로 검색한다. */
	@GetMapping
	fun search(
		@RequestParam type: SchoolSearchType,
		@RequestParam @NotBlank(message = "필수 입력 항목입니다.") @Size(max = 253) query: String,
	): ApiResponse<List<SchoolSearchResponse>> =
		ApiResponse.success(searchSchoolsService.search(SearchSchoolsQuery(type, query)).map(SchoolSearchResponse::from))
}
