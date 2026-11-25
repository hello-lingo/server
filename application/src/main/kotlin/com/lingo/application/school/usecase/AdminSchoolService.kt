package com.lingo.application.school.usecase

import com.lingo.application.school.exception.SchoolDomainNotFoundException
import com.lingo.application.school.exception.SchoolNotFoundException
import com.lingo.application.school.port.out.SchoolRepositoryPort
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자의 학교·도메인 관리.
 * 관리자 권한은 URL 규칙(/api/v1/admin 아래 전체)이 검사한다.
 * 이름 중복은 저장소의 고유 제약이 DuplicateSchoolNameException으로 알린다.
 */
@Service
@Transactional
class AdminSchoolService(
	private val schoolRepositoryPort: SchoolRepositoryPort,
) {

	@Transactional(readOnly = true)
	fun listSchools(query: String?): List<AdminSchoolResult> {
		val keyword = query?.trim()?.lowercase().orEmpty()
		return schoolRepositoryPort.findAll()
			.filter { school ->
				keyword.isEmpty() ||
					school.name.value.lowercase().contains(keyword) ||
					school.domains.any { it.domain.value.contains(keyword) }
			}
			.sortedBy { it.name.value }
			.map(AdminSchoolResult::from)
	}

	fun addSchool(command: AddSchoolCommand): AdminSchoolResult {
		val school = School.create(SchoolName.of(command.name), command.domains.map(SchoolDomain::of))
		return AdminSchoolResult.from(schoolRepositoryPort.save(school))
	}

	fun updateSchool(command: UpdateSchoolCommand): AdminSchoolResult {
		var school = findSchool(command.schoolId)
		command.name?.let { school = school.rename(SchoolName.of(it)) }
		command.hidden?.let { school = school.changeVisibility(it) }
		return AdminSchoolResult.from(schoolRepositoryPort.save(school))
	}

	fun deleteSchool(schoolId: Long) {
		findSchool(schoolId)
		// TODO(학력·증명서 PR): 학력이나 끝나지 않은 증명서 신청이 가리키면 거부하고 숨김을 안내한다.
		schoolRepositoryPort.deleteById(schoolId)
	}

	fun addDomain(command: AddSchoolDomainCommand): AdminSchoolResult {
		val school = findSchool(command.schoolId)
		return AdminSchoolResult.from(schoolRepositoryPort.save(school.addDomain(SchoolDomain.of(command.domain))))
	}

	fun changeDomain(command: ChangeSchoolDomainCommand): AdminSchoolResult {
		val school = findSchoolWithDomain(command.schoolId, command.domainId)
		return AdminSchoolResult.from(schoolRepositoryPort.save(school.changeDomain(command.domainId, SchoolDomain.of(command.domain))))
	}

	fun removeDomain(schoolId: Long, domainId: Long): AdminSchoolResult {
		val school = findSchoolWithDomain(schoolId, domainId)
		return AdminSchoolResult.from(schoolRepositoryPort.save(school.removeDomain(domainId)))
	}

	private fun findSchool(schoolId: Long): School =
		schoolRepositoryPort.findById(schoolId) ?: throw SchoolNotFoundException(debugMessage = "schoolId=$schoolId")

	private fun findSchoolWithDomain(schoolId: Long, domainId: Long): School {
		val school = findSchool(schoolId)
		if (!school.hasDomain(domainId)) {
			throw SchoolDomainNotFoundException(debugMessage = "schoolId=$schoolId, domainId=$domainId")
		}
		return school
	}
}
