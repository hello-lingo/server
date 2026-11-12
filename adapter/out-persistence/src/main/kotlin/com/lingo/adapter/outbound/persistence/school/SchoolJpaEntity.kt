package com.lingo.adapter.outbound.persistence.school

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "schools", uniqueConstraints = [UniqueConstraint(name = SchoolJpaEntity.UK_NAME, columnNames = ["name"])])
class SchoolJpaEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(nullable = false, length = 100)
	var name: String,

	@Column(nullable = false)
	var hidden: Boolean,

	// 도메인은 학교와 생명주기가 같음, 목록에서 빠진 도메인은 행도 지움
	@OneToMany(mappedBy = "school", cascade = [CascadeType.ALL], orphanRemoval = true)
	@OrderBy("id")
	var domains: MutableList<SchoolDomainJpaEntity> = mutableListOf(),
) {
	companion object {
		const val UK_NAME = "uk_schools_name"
	}
}
