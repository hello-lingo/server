package com.lingo.config

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 모든 응답에 현재 요청의 추적 ID를 X-Trace-Id 헤더로 내려준다.
 * 추적 시작 필터(HIGHEST_PRECEDENCE + 1) 바로 다음에 실행되어 401처럼 보안 필터에서 끝나는 응답에도 붙는다.
 * 현재 span이 없으면(추적이 꺼진 경우 등) 헤더를 붙이지 않는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
class TraceIdResponseFilter(private val tracer: Tracer) : OncePerRequestFilter() {

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
		// 체인 진행 전에 붙여야 응답이 커밋된 뒤(본문 작성 후)에도 헤더가 남는다
		tracer.currentSpan()?.context()?.traceId()?.let { response.setHeader(TRACE_ID_HEADER, it) }
		chain.doFilter(request, response)
	}

	companion object {
		const val TRACE_ID_HEADER = "X-Trace-Id"
	}
}
