package com.lingo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
		http
			.csrf { it.disable() }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests {
				it.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
				it.anyRequest().authenticated()
			}
			.build()
}
