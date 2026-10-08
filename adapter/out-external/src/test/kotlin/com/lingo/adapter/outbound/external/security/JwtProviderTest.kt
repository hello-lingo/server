package com.lingo.adapter.outbound.external.security

import com.lingo.application.user.model.AuthenticatedUser
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtProviderTest {

	private val secret = "test-secret-test-secret-test-secret-32b"
	private val now = Instant.parse("2026-01-01T00:00:00Z")
	private val user = AuthenticatedUser(userId = 7, email = "a@b.com")

	private fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

	private fun provider(secret: String = this.secret, at: Instant = now) =
		JwtProvider(secret, 3600, clockAt(at))

	private fun payloadOf(token: String): String =
		String(Base64.getUrlDecoder().decode(token.split(".")[1]))

	@Test
	fun `J1 발급한 토큰을 검증하면 사용자 정보가 복원되고 expiresIn은 TTL이다`() {
		val issued = provider().issue(user)

		assertEquals(3600, issued.expiresIn)
		val authenticated = assertNotNull(provider().verify(issued.token))
		assertEquals(7, authenticated.userId)
		assertEquals("a@b.com", authenticated.email)
	}

	@Test
	fun `J2 TTL 직전까지 유효하고 TTL 이후에는 null이다`() {
		val token = provider().issue(user).token

		assertNotNull(provider(at = now.plusSeconds(3599)).verify(token))
		assertNull(provider(at = now.plusSeconds(3601)).verify(token))
	}

	@Test
	fun `J3 다른 secret으로 서명된 토큰은 null이다`() {
		val foreign = provider(secret = "another-secret-another-secret-32bytes!").issue(user).token

		assertNull(provider().verify(foreign))
	}

	@Test
	fun `J4 변조 빈 문자열 형식 오류 alg none 토큰은 모두 null이고 예외가 나가지 않는다`() {
		val token = provider().issue(user).token
		val parts = token.split(".")
		val tamperedPayload = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(String(Base64.getUrlDecoder().decode(parts[1])).replace("\"7\"", "\"8\"").toByteArray())
		val tampered = "${parts[0]}.$tamperedPayload.${parts[2]}"
		val enc = Base64.getUrlEncoder().withoutPadding()
		val noneToken = enc.encodeToString("""{"alg":"none"}""".toByteArray()) + "." +
			enc.encodeToString("""{"sub":"7","email":"a@b.com"}""".toByteArray()) + "."

		listOf(tampered, "", "abc", noneToken).forEach { assertNull(provider().verify(it), it) }
	}

	@Test
	fun `J5 secret이 32바이트 미만이면 생성에 실패한다`() {
		assertFailsWith<IllegalArgumentException> { provider(secret = "short-secret") }
	}

	@Test
	fun `J6 payload는 sub와 email만 담고 해시를 담지 않는다`() {
		val payload = payloadOf(provider().issue(user).token)

		assertContains(payload, "\"sub\":\"7\"")
		assertContains(payload, "\"email\":\"a@b.com\"")
		assertFalse(payload.contains("secrethash"))
	}

	@Test
	fun `J7 issue 후 만료 시각은 TTL만큼 뒤다`() {
		val payload = payloadOf(provider().issue(user).token)

		assertContains(payload, "\"exp\":${now.plus(Duration.ofHours(1)).epochSecond}")
	}
}
