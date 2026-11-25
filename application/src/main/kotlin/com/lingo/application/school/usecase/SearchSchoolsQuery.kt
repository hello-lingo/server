package com.lingo.application.school.usecase

data class SearchSchoolsQuery(
	val type: SchoolSearchType,
	val keyword: String,
)
