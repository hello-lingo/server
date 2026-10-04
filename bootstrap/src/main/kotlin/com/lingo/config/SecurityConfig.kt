package com.lingo.config

import com.lingo.application.user.port.out.TokenProviderPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.json.JsonMapper

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(
		http: HttpSecurity,
		tokenProviderPort: TokenProviderPort,
		jsonMapper: JsonMapper,
	): SecurityFilterChain =
		http
			.csrf { it.disable() }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.exceptionHandling { it.authenticationEntryPoint(JsonAuthenticationEntryPoint(jsonMapper)) }
			.authorizeHttpRequests {
				it.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
				it.anyRequest().authenticated()
			}
			.addFilterBefore(JwtAuthenticationFilter(tokenProviderPort), UsernamePasswordAuthenticationFilter::class.java)
			.build()
}
