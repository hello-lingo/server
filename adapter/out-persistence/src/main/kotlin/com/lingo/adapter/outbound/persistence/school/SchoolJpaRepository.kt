package com.lingo.adapter.outbound.persistence.school

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SchoolJpaRepository : JpaRepository<SchoolJpaEntity, Long> {
	// 학교마다 도메인을 따로 읽지 않도록 한 번에 가져온다
	@Query("select distinct s from SchoolJpaEntity s left join fetch s.domains order by s.id")
	fun findAllWithDomains(): List<SchoolJpaEntity>
}
