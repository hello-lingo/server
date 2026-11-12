package com.lingo.application.school.usecase

data class AddSchoolCommand(
	val name: String,
	val domains: List<String>,
)

/** null인 값은 바꾸지 않는다 */
data class UpdateSchoolCommand(
	val schoolId: Long,
	val name: String?,
	val hidden: Boolean?,
)

data class AddSchoolDomainCommand(
	val schoolId: Long,
	val domain: String,
)

data class ChangeSchoolDomainCommand(
	val schoolId: Long,
	val domainId: Long,
	val domain: String,
)
