package com.lingo.adapter.outbound.persistence

import com.lingo.application.user.DuplicateEmailException
import com.lingo.domain.user.Email
import com.lingo.domain.user.User
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DataJpaTest
@Import(UserPersistenceAdapter::class)
class UserPersistenceAdapterTest @Autowired constructor(
	private val adapter: UserPersistenceAdapter,
	private val repository: UserJpaRepository,
	private val entityManager: EntityManager,
) {

	private fun newUser(email: String = "a@b.com", hash: String = "\$2a\$10\$hash") =
		User.signUp(Email.of(email), hash, "홍길동")

	@Test
	fun `R1 저장하면 id가 채워지고 재조회 값이 일치한다`() {
		val saved = adapter.save(newUser())
		entityManager.clear()

		val id = assertNotNull(saved.id)
		val found = repository.findById(id).get()
		assertEquals("a@b.com", found.email)
		assertEquals("홍길동", found.name)
		assertEquals(saved.email.value, found.email)
	}

	@Test
	fun `R2 저장된 이메일은 true 없는 이메일은 false`() {
		adapter.save(newUser("a@b.com"))

		assertTrue(adapter.existsByEmail(Email.of("a@b.com")))
		assertFalse(adapter.existsByEmail(Email.of("x@b.com")))
	}

	@Test
	fun `R3 같은 이메일을 다시 저장하면 DuplicateEmailException이고 cause가 보존된다`() {
		adapter.save(newUser("a@b.com"))

		val thrown = assertFailsWith<DuplicateEmailException> { adapter.save(newUser("a@b.com")) }

		assertIs<DataIntegrityViolationException>(thrown.cause)
	}

	@Test
	fun `R4 비밀번호 해시는 그대로 저장된다`() {
		val saved = adapter.save(newUser(hash = "\$2a\$10\$abcdefg"))
		entityManager.clear()

		assertEquals("\$2a\$10\$abcdefg", repository.findById(assertNotNull(saved.id)).get().passwordHash)
	}
}
