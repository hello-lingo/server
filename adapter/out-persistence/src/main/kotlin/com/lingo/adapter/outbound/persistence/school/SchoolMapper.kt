package com.lingo.adapter.outbound.persistence.school

import com.lingo.domain.school.RegisteredDomain
import com.lingo.domain.school.School
import com.lingo.domain.school.SchoolDomain
import com.lingo.domain.school.SchoolName

internal object SchoolMapper {

	fun toEntity(school: School): SchoolJpaEntity {
		val entity = SchoolJpaEntity(id = school.id, name = school.name.value, hidden = school.hidden)
		school.domains.forEach { entity.domains.add(SchoolDomainJpaEntity(id = it.id, school = entity, domain = it.domain.value)) }
		return entity
	}

	fun toDomain(entity: SchoolJpaEntity): School =
		School(
			id = entity.id,
			name = SchoolName.of(entity.name),
			domains = entity.domains.map { RegisteredDomain(id = it.id, domain = SchoolDomain.of(it.domain)) },
			hidden = entity.hidden,
		)
}
