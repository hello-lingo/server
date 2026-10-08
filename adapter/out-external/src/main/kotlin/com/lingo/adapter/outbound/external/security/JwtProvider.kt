package com.lingo.adapter.outbound.external.security

import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.model.IssuedAccessJwt
import com.lingo.application.user.port.out.JwtProviderPort
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.Date

@Component
class JwtProvider(
	@Value("\${lingo.auth.jwt.secret}") secret: String,
	@Value("\${lingo.auth.jwt.access-ttl-seconds}") private val accessTtlSeconds: Long,
	private val clock: Clock,
) : JwtProviderPort {

	private val key = secret.toByteArray(Charsets.UTF_8).also {
		require(it.size >= MIN_SECRET_BYTES) { "JWT secret은 최소 ${MIN_SECRET_BYTES}바이트여야 합니다." }
	}.let(Keys::hmacShaKeyFor)

	private val parser = Jwts.parser()
		.verifyWith(key)
		.clock { Date.from(clock.instant()) }
		.build()

	override fun issue(principal: AuthenticatedUser): IssuedAccessJwt {
		val issuedAt = clock.instant()
		val token = Jwts.builder()
			.subject(principal.userId.toString())
			.claim(EMAIL_CLAIM, principal.email)
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(issuedAt.plusSeconds(accessTtlSeconds)))
			.signWith(key)
			.compact()
		return IssuedAccessJwt(token, accessTtlSeconds)
	}

	override fun verify(token: String): AuthenticatedUser? =
		try {
			val claims = parser.parseSignedClaims(token).payload
			val userId = claims.subject?.toLongOrNull()
			val email = claims.get(EMAIL_CLAIM, String::class.java)
			if (userId == null || email == null) null else AuthenticatedUser(userId, email)
		} catch (e: JwtException) {
			null
		} catch (e: IllegalArgumentException) {
			null
		}

	private companion object {
		const val EMAIL_CLAIM = "email"
		const val MIN_SECRET_BYTES = 32
	}
}
