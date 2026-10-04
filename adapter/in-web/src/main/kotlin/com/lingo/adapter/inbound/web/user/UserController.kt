package com.lingo.adapter.inbound.web.user

import com.lingo.adapter.inbound.web.common.ApiResponse
import com.lingo.adapter.inbound.web.common.AuthRequestAttributes
import com.lingo.adapter.inbound.web.user.request.LoginRequest
import com.lingo.adapter.inbound.web.user.request.SignUpRequest
import com.lingo.adapter.inbound.web.user.response.LoginResponse
import com.lingo.adapter.inbound.web.user.response.MeResponse
import com.lingo.adapter.inbound.web.user.response.SignUpResponse
import com.lingo.application.user.AuthenticatedUser
import com.lingo.application.user.usecase.LoginService
import com.lingo.application.user.usecase.SignUpService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class UserController(
	private val signUpService: SignUpService,
	private val loginService: LoginService,
) {

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	fun signUp(@Valid @RequestBody request: SignUpRequest): ApiResponse<SignUpResponse> {
		val result = signUpService.signUp(request.toCommand())
		return ApiResponse.success(SignUpResponse(userId = result.userId, email = result.email))
	}

	@PostMapping("/login")
	fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<LoginResponse> {
		val result = loginService.login(request.toCommand())
		return ApiResponse.success(LoginResponse(accessToken = result.accessToken, expiresIn = result.expiresIn))
	}

	@GetMapping("/me")
	fun me(@RequestAttribute(AuthRequestAttributes.AUTHENTICATED_USER) user: AuthenticatedUser): ApiResponse<MeResponse> =
		ApiResponse.success(MeResponse(userId = user.userId, email = user.email))
}
