package com.lingo.adapter.inbound.web.common

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/** @AuthenticationPrincipal 리졸버 등록용 테스트 설정. 인가는 열어두고 컨트롤러 동작만 검증한다. */
@TestConfiguration
@EnableWebSecurity
class PermitAllSecurityConfig {
	@Bean
	fun filterChain(http: HttpSecurity): SecurityFilterChain =
		http.csrf { it.disable() }.authorizeHttpRequests { it.anyRequest().permitAll() }.build()
}
