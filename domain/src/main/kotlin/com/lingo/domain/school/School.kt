package com.lingo.domain.school

/**
 * 학교와 그 학교의 이메일 도메인 목록
 * - 도메인은 1개 이상이어야 한음
 * - 마지막 도메인은 지울 수 없음
 * - 한 학교 안에서 도메인은 겹치지 않음
 * - 다른 학교와의 중복은 DB 고유 제약으로 막음
 * - hidden이면 검색에서 빠짐
 */
data class School(
	val id: Long?,
	val name: SchoolName,
	val domains: List<RegisteredDomain>,
	val hidden: Boolean,
) {

	init {
		if (domains.isEmpty()) {
			throw SchoolDomainRequiredException("학교에는 도메인이 1개 이상 있어야 합니다.")
		}
		if (domains.map { it.domain }.toSet().size != domains.size) {
			throw DuplicateDomainInSchoolException("학교에 같은 도메인이 이미 있습니다.")
		}
	}

	fun rename(newName: SchoolName): School = copy(name = newName)

	fun changeVisibility(hidden: Boolean): School = copy(hidden = hidden)

	fun hasDomain(domainId: Long): Boolean = domains.any { it.id == domainId }

	fun addDomain(domain: SchoolDomain): School = copy(domains = domains + RegisteredDomain(id = null, domain = domain))

	/**
	 * 없는 domainId는 호출하는 쪽의 실수니까
	 * 서비스가 [hasDomain]으로 먼저 확인하고 404로 응답한다
	 */
	fun changeDomain(domainId: Long, newDomain: SchoolDomain): School {
		require(hasDomain(domainId)) { "학교 ${id}에 도메인 ${domainId}가 없습니다." }
		return copy(domains = domains.map { if (it.id == domainId) it.copy(domain = newDomain) else it })
	}

	fun removeDomain(domainId: Long): School {
		require(hasDomain(domainId)) { "학교 ${id}에 도메인 ${domainId}가 없습니다." }
		if (domains.size == 1) {
			throw LastSchoolDomainException("학교의 마지막 도메인은 삭제할 수 없습니다.")
		}
		return copy(domains = domains.filterNot { it.id == domainId })
	}

	companion object {
		fun create(name: SchoolName, domains: List<SchoolDomain>): School =
			School(id = null, name = name, domains = domains.map { RegisteredDomain(id = null, domain = it) }, hidden = false)
	}
}
