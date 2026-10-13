package com.lingo.application.user.port.out

import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.model.IssuedAccessJwt

interface JwtProviderPort {
	fun issue(principal: AuthenticatedUser): IssuedAccessJwt

	/** 위조·만료·형식 오류, `role` 누락이나 모르는 `role` 값 토큰은 모두 null을 반환한다. */
	fun verify(token: String): AuthenticatedUser?
}
