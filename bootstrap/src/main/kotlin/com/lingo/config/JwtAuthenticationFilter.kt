package com.lingo.config

import com.lingo.application.user.port.out.JwtProviderPort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Bearer 토큰을 검증해 SecurityContext에 인증 사용자를 심는다.
 * 토큰이 없거나 유효하지 않으면 아무것도 세팅하지 않고 체인을 진행한다(차단은 AuthenticationEntryPoint 몫).
 * SecurityConfig에서만 등록하므로 빈으로 만들지 않는다.
 */
class JwtAuthenticationFilter(
	private val tokenProviderPort: JwtProviderPort,
) : OncePerRequestFilter() {

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
		try {
			extractToken(request)?.let(tokenProviderPort::verify)?.let { user ->
				val authentication = UsernamePasswordAuthenticationToken(user, null, listOf(SimpleGrantedAuthority(ROLE_USER)))
				SecurityContextHolder.getContext().authentication = authentication
			}
			chain.doFilter(request, response)
		} finally {
			SecurityContextHolder.clearContext()
		}
	}

	private fun extractToken(request: HttpServletRequest): String? =
		request.getHeader(HttpHeaders.AUTHORIZATION)
			?.takeIf { it.startsWith(BEARER_PREFIX) }
			?.substring(BEARER_PREFIX.length)
			?.trim()
			?.takeIf { it.isNotEmpty() }

	private companion object {
		const val BEARER_PREFIX = "Bearer "
		const val ROLE_USER = "ROLE_USER"
	}
}
