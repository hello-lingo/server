package com.lingo.adapter.outbound.persistence.school

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * 학교 검색용 전체 목록 캐시
 *
 * 비우기·채우기를 트랜잭션 커밋 뒤로 미룬다.
 * 바로 비우면 커밋 전에 다른 요청이 옛 목록을 다시 읽어 캐시에 넣고,
 * 그 값이 만료(10분)까지 남을 수 있다.
 */
@Configuration
@EnableCaching
class SchoolCacheConfig {

	@Bean
	fun cacheManager(): CacheManager {
		val caffeineCacheManager = CaffeineCacheManager(SCHOOLS).apply {
			setCaffeine(Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(1))
		}
		return TransactionAwareCacheManagerProxy(caffeineCacheManager)
	}

	companion object {
		const val SCHOOLS = "schools"

		/** 서버가 여러 대일 때 다른 서버의 변경이 늦어도 이 시간 안에는 반영된다 */
		val TTL: Duration = Duration.ofMinutes(10)
	}
}
