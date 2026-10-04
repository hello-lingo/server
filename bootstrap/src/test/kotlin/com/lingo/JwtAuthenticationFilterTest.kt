package com.lingo

import com.lingo.adapter.inbound.web.common.AuthRequestAttributes
import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.model.IssuedAccessJwt
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.config.JwtAuthenticationFilter
import com.lingo.domain.user.User
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtAuthenticationFilterTest {

	private val user = AuthenticatedUser(7, "a@b.com")
	private val tokens = object : JwtProviderPort {
		override fun issue(user: User): IssuedAccessJwt = error("unused")
		override fun verify(token: String): AuthenticatedUser? = if (token == "valid") user else null
	}
	private val filter = JwtAuthenticationFilter(tokens)

	@AfterTest
	fun clear() = SecurityContextHolder.clearContext()

	private class CapturingChain : jakarta.servlet.FilterChain {
		var invoked = false
		var authentication: Any? = null
		override fun doFilter(request: jakarta.servlet.ServletRequest, response: jakarta.servlet.ServletResponse) {
			invoked = true
			authentication = SecurityContextHolder.getContext().authentication
		}
	}

	private fun run(header: String?): Pair<MockHttpServletRequest, CapturingChain> {
		val request = MockHttpServletRequest()
		header?.let { request.addHeader("Authorization", it) }
		val chain = CapturingChain()
		filter.doFilter(request, MockHttpServletResponse(), chain)
		return request to chain
	}

	@Test
	fun `F1 유효한 Bearer 토큰이면 인증 정보와 request attribute가 세팅되고 체인이 진행된다`() {
		val (request, chain) = run("Bearer valid")

		assertEquals(true, chain.invoked)
		val authentication = assertNotNull(chain.authentication as? org.springframework.security.core.Authentication)
		assertEquals(user, authentication.principal)
		assertEquals(listOf("ROLE_USER"), authentication.authorities.map { it.authority })
		assertEquals(user, request.getAttribute(AuthRequestAttributes.AUTHENTICATED_USER))
	}

	@Test
	fun `F2 헤더 없음 Basic 빈 토큰 검증 실패는 인증을 세팅하지 않고 체인은 진행된다`() {
		listOf(null, "Basic abc", "Bearer ", "Bearer invalid").forEach { header ->
			val (request, chain) = run(header)

			assertEquals(true, chain.invoked, header)
			assertNull(chain.authentication, header)
			assertNull(request.getAttribute(AuthRequestAttributes.AUTHENTICATED_USER), header)
		}
	}

	@Test
	fun `F3 요청이 끝나면 SecurityContext에 인증이 남지 않는다`() {
		run("Bearer valid")
		assertNull(SecurityContextHolder.getContext().authentication)

		val (_, second) = run(null)
		assertNull(second.authentication)
	}
}
