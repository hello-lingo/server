package com.lingo.adapter.outbound.persistence.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
	fun existsByEmail(email: String): Boolean

	fun findByEmail(email: String): UserJpaEntity?
}
