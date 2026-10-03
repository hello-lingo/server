package com.lingo

import com.lingo.application.user.port.out.ExistsUserByEmailPort
import com.lingo.application.user.port.out.PasswordEncoderPort
import com.lingo.application.user.port.out.SaveUserPort
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignUpIntegrationTest @Autowired constructor(
	private val mockMvc: MockMvc,
	private val jdbcTemplate: JdbcTemplate,
	private val context: ApplicationContext,
) {

	@BeforeEach
	fun cleanUsers() {
		jdbcTemplate.update("delete from users")
	}

	private fun signUp(email: String = "a@b.com", password: String = "Abcdef1!") =
		mockMvc.perform(
			post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$password","name":"홍길동"}"""),
		)

	@Test
	fun `I0 포트마다 구현 빈이 정확히 하나씩 주입된다`() {
		listOf(SaveUserPort::class.java, ExistsUserByEmailPort::class.java, PasswordEncoderPort::class.java)
			.forEach { port -> assertEquals(1, context.getBeansOfType(port).size, port.simpleName) }
	}

	@Test
	fun `I1 인증 없이 가입하면 201이고 BCrypt 해시가 저장된다`() {
		signUp()
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.userId").isNumber)
			.andExpect(jsonPath("$.email").value("a@b.com"))

		val hash = jdbcTemplate.queryForObject("select password_hash from users where email = 'a@b.com'", String::class.java)!!
		assertNotEquals("Abcdef1!", hash)
		assertTrue(hash.startsWith("\$2"))
	}

	@Test
	fun `I2 같은 이메일은 대소문자가 달라도 409`() {
		signUp("a@b.com").andExpect(status().isCreated)

		signUp("A@B.com").andExpect(status().isConflict).andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
	}

	@Test
	fun `I3 정책 위반 비밀번호는 400`() {
		signUp(password = "short").andExpect(status().isBadRequest)
	}

	@Test
	fun `I4 signup 이외 경로는 보호된다`() {
		mockMvc.perform(get("/api/v1/anything")).andExpect(status().isForbidden)
	}
}
