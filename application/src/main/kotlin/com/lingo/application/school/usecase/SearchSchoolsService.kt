package com.lingo.application.school.usecase

import com.lingo.application.school.port.out.SchoolRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 학교–도메인 목록 검색
 * 전체 목록을 받아 메모리에서 거르고 정렬한다.
 * 목록 캐시는 저장소 어댑터가 맡는다.
 */
@Service
@Transactional(readOnly = true)
class SearchSchoolsService(
	private val schoolRepositoryPort: SchoolRepositoryPort,
) {

	fun search(query: SearchSchoolsQuery): List<SchoolSearchResult> {
		val keyword = query.keyword.trim().lowercase()
		if (keyword.isEmpty()) {
			return emptyList()
		}

		val lines = schoolRepositoryPort.findAll()
			.filterNot { it.hidden }
			.flatMap { school ->
				school.domains.map { registered ->
					SchoolSearchResult(
						schoolId = requireNotNull(school.id) { "저장된 학교에 id가 없습니다." },
						schoolName = school.name.value,
						domainId = requireNotNull(registered.id) { "저장된 도메인에 id가 없습니다." },
						domain = registered.domain.value,
					)
				}
			}

		val target: (SchoolSearchResult) -> String = when (query.type) {
			SchoolSearchType.NAME -> { line -> line.schoolName.lowercase() }
			SchoolSearchType.DOMAIN -> { line -> line.domain }
		}
		val tieBreaker: (SchoolSearchResult) -> String = when (query.type) {
			SchoolSearchType.NAME -> SchoolSearchResult::domain
			SchoolSearchType.DOMAIN -> SchoolSearchResult::schoolName
		}

		// 시작 일치 묶음 → 중간 포함 묶음, 각 묶음 안에서 검색 대상 오름차순
		return lines
			.filter { target(it).contains(keyword) }
			.sortedWith(
				compareBy<SchoolSearchResult> { !target(it).startsWith(keyword) }
					.thenBy(target)
					.thenBy(tieBreaker),
			)
			.take(MAX_RESULTS)
	}

	companion object {
		/** 드롭다운에 보이는 줄 수 상한 */
		const val MAX_RESULTS = 20
	}
}
