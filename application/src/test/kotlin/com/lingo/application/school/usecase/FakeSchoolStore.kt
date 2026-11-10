package com.lingo.application.school.usecase

import com.lingo.application.school.port.out.SchoolRepositoryPort
import com.lingo.domain.school.RegisteredDomain
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName

class FakeSchoolStore : SchoolRepositoryPort {
	val schools = mutableListOf<School>()
	var findAllCalls = 0
	private var schoolSequence = 0L
	private var domainSequence = 0L

	/** 테스트 준비용: 이름과 도메인으로 저장된 학교를 바로 만든다 */
	fun given(name: String, vararg domains: String, hidden: Boolean = false): School =
		save(School.create(SchoolName.of(name), domains.map(SchoolDomain::of)).changeVisibility(hidden))

	override fun save(school: School): School {
		val saved = school.copy(
			id = school.id ?: ++schoolSequence,
			domains = school.domains.map { if (it.id == null) RegisteredDomain(++domainSequence, it.domain) else it },
		)
		schools.removeIf { it.id == saved.id }
		schools += saved
		return saved
	}

	override fun findById(id: Long): School? = schools.firstOrNull { it.id == id }

	override fun findAll(): List<School> {
		findAllCalls++
		return schools.sortedBy { it.id }
	}

	override fun deleteById(id: Long) {
		schools.removeIf { it.id == id }
	}
}
