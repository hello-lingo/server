package com.lingo.adapter.inbound.web.school

import com.lingo.adapter.inbound.web.common.ApiResponse
import com.lingo.adapter.inbound.web.school.request.AddSchoolRequest
import com.lingo.adapter.inbound.web.school.request.SchoolDomainRequest
import com.lingo.adapter.inbound.web.school.request.UpdateSchoolRequest
import com.lingo.adapter.inbound.web.school.response.AdminSchoolResponse
import com.lingo.application.school.usecase.AddSchoolDomainCommand
import com.lingo.application.school.usecase.AdminSchoolService
import com.lingo.application.school.usecase.ChangeSchoolDomainCommand
import com.lingo.application.school.usecase.UpdateSchoolCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 학교·도메인 관리
 * ADMIN 권한은 SecurityConfig의 /api/v1/admin 경로 규칙이 검사한다
 * */
@RestController
@RequestMapping("/api/v1/admin/schools")
class AdminSchoolController(
	private val adminSchoolService: AdminSchoolService,
) {

	/** 숨긴 학교 포함, 이름순. query가 있으면 이름이나 도메인에 포함된 학교만 */
	@GetMapping
	fun list(@RequestParam(required = false) query: String?): ApiResponse<List<AdminSchoolResponse>> =
		ApiResponse.success(adminSchoolService.listSchools(query).map(AdminSchoolResponse::from))

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun add(@Valid @RequestBody request: AddSchoolRequest): ApiResponse<AdminSchoolResponse> =
		ApiResponse.success(AdminSchoolResponse.from(adminSchoolService.addSchool(request.toCommand())))

	@PatchMapping("/{schoolId}")
	fun update(@PathVariable schoolId: Long, @RequestBody request: UpdateSchoolRequest): ApiResponse<AdminSchoolResponse> =
		ApiResponse.success(
			AdminSchoolResponse.from(adminSchoolService.updateSchool(UpdateSchoolCommand(schoolId, request.name, request.hidden))),
		)

	@DeleteMapping("/{schoolId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@PathVariable schoolId: Long) {
		adminSchoolService.deleteSchool(schoolId)
	}

	@PostMapping("/{schoolId}/domains")
	@ResponseStatus(HttpStatus.CREATED)
	fun addDomain(@PathVariable schoolId: Long, @Valid @RequestBody request: SchoolDomainRequest): ApiResponse<AdminSchoolResponse> =
		ApiResponse.success(AdminSchoolResponse.from(adminSchoolService.addDomain(AddSchoolDomainCommand(schoolId, request.domain))))

	@PutMapping("/{schoolId}/domains/{domainId}")
	fun changeDomain(
		@PathVariable schoolId: Long,
		@PathVariable domainId: Long,
		@Valid @RequestBody request: SchoolDomainRequest,
	): ApiResponse<AdminSchoolResponse> =
		ApiResponse.success(
			AdminSchoolResponse.from(adminSchoolService.changeDomain(ChangeSchoolDomainCommand(schoolId, domainId, request.domain))),
		)

	@DeleteMapping("/{schoolId}/domains/{domainId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun removeDomain(@PathVariable schoolId: Long, @PathVariable domainId: Long) {
		adminSchoolService.removeDomain(schoolId, domainId)
	}
}
