package com.lingo.adapter.outbound.persistence.school

import com.lingo.application.school.exception.DuplicateSchoolNameException
import com.lingo.application.school.port.out.SchoolRepositoryPort
import com.lingo.domain.school.School
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class SchoolPersistenceAdapter(
	private val repository: SchoolJpaRepository,
) : SchoolRepositoryPort {

	// id가 있으면 merge되어 도메인 목록을 통째로 맞춘다 (빠진 도메인은 orphanRemoval로 삭제)
	override fun save(school: School): School =
		try {
			SchoolMapper.toDomain(repository.saveAndFlush(SchoolMapper.toEntity(school)))
		} catch (e: DataIntegrityViolationException) {
			// 이름 고유 제약만 사용자 오류로 바꾸고, 다른 제약 위반은 예상하지 못한 것이므로 그대로 둔다
			if (e.mostSpecificCause.message?.contains(SchoolJpaEntity.UK_NAME, ignoreCase = true) == true) {
				throw DuplicateSchoolNameException(debugMessage = "${SchoolJpaEntity.UK_NAME} 고유 제약 위반", cause = e)
			}
			throw e
		}

	override fun findById(id: Long): School? = repository.findById(id).orElse(null)?.let(SchoolMapper::toDomain)

	override fun findAll(): List<School> = repository.findAllWithDomains().map(SchoolMapper::toDomain)

	override fun deleteById(id: Long) = repository.deleteById(id)
}
