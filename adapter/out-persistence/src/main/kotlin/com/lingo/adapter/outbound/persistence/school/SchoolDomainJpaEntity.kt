package com.lingo.adapter.outbound.persistence.school

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
	name = "school_domains",
	uniqueConstraints = [UniqueConstraint(name = "uk_school_domains_school_domain", columnNames = ["school_id", "domain"])],
)
class SchoolDomainJpaEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "school_id", nullable = false)
	var school: SchoolJpaEntity,

	@Column(nullable = false, length = 253)
	var domain: String,
)
