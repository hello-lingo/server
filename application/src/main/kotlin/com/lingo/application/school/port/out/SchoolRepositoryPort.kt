package com.lingo.application.school.port.out

import com.lingo.domain.school.School

interface SchoolRepositoryPort {
	/** 새 학교는 추가, id가 있으면 도메인 목록까지 그대로 덮어쓴다. 이름이 겹치면 DuplicateSchoolNameException */
	fun save(school: School): School

	fun findById(id: Long): School?

	/** 숨긴 학교를 포함한 전체 목록 (id 오름차순). 검색은 이 목록을 메모리에서 거른다 (D9) */
	fun findAll(): List<School>

	/** 학교와 그 도메인을 함께 지운다. 없는 id면 아무것도 하지 않는다 */
	fun deleteById(id: Long)
}
