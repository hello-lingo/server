package com.lingo.adapter.inbound.web.school.request

import com.lingo.application.school.usecase.AddSchoolCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/** 학교는 도메인과 함께만 추가한다. 형식 검사는 도메인 값 객체가 한다. */
data class AddSchoolRequest(
	@field:NotBlank(message = "필수 입력 항목입니다.") val name: String,
	@field:NotEmpty(message = "도메인을 1개 이상 입력해야 합니다.") val domains: List<String>,
) {
	fun toCommand() = AddSchoolCommand(name = name, domains = domains)
}

/** 보낸 값만 바꾼다 */
data class UpdateSchoolRequest(
	val name: String? = null,
	val hidden: Boolean? = null,
)

data class SchoolDomainRequest(
	@field:NotBlank(message = "필수 입력 항목입니다.") val domain: String,
)
