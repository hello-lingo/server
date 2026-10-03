package com.lingo.adapter.inbound.web

import com.lingo.application.user.SignUpService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class SignUpController(
	private val signUpService: SignUpService,
) {

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	fun signUp(@Valid @RequestBody request: SignUpRequest): SignUpResponse {
		val result = signUpService.signUp(request.toCommand())
		return SignUpResponse(userId = result.userId, email = result.email)
	}
}
