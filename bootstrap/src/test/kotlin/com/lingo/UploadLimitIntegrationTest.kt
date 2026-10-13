package com.lingo

import com.jayway.jsonpath.JsonPath
import com.lingo.adapter.inbound.web.common.UploadLimits
import com.lingo.application.user.model.AuthenticatedUser
import com.lingo.application.user.port.out.JwtProviderPort
import com.lingo.domain.user.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.unit.DataSize
import org.springframework.web.client.RestClient
import testsupport.UploadProbeController
import kotlin.test.Test
import kotlin.test.assertEquals

// MockMvc는 업로드 한도를 적용하지 않으므로 실제 Tomcat(RANDOM_PORT)으로 보낸다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(UploadProbeController::class)
class UploadLimitIntegrationTest @Autowired constructor(
	@LocalServerPort private val port: Int,
	private val jwtProviderPort: JwtProviderPort,
	@Value("\${spring.servlet.multipart.max-file-size}") private val maxFileSize: DataSize,
) {

	// 토큰이 없으면 multipart를 해석하기 전에 401이 난다. 인증 필터는 DB를 조회하지 않으므로 사용자 행이 없어도 된다
	private val token = jwtProviderPort.issue(AuthenticatedUser(1, "a@b.com", Role.USER)).token

	private fun upload(size: Int): Pair<HttpStatusCode, String?> {
		// filename이 없으면 일반 폼 필드로 전송되어 MultipartFile로 받을 수 없다.
		// MultipartBodyBuilder는 reactive-streams(WebFlux용)가 필요해 이 모듈에서는 NoClassDefFoundError → 파트 헤더를 직접 만든다
		val headers = HttpHeaders().apply { contentDisposition = ContentDisposition.formData().name("file").filename("certificate.bin").build() }
		val body = LinkedMultiValueMap<String, Any>().apply { add("file", HttpEntity(ByteArray(size), headers)) }
		return RestClient.create("http://localhost:$port").post()
			.uri("/api/v1/upload-probe")
			.header("Authorization", "Bearer $token")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(body)
			.exchange { _, response -> response.statusCode to response.bodyTo(String::class.java) } // retrieve()는 4xx에서 예외를 던진다
	}

	@Test
	fun `UP1 한도 이하(9MB) 파일은 200이고 크기를 그대로 받는다`() {
		val (status, body) = upload(9 * 1024 * 1024)

		assertEquals(200, status.value(), body)
		assertEquals(9 * 1024 * 1024, JsonPath.read<Int>(body, "$.data.size"))
	}

	@Test
	fun `UP2 한도 초과(11MB) 파일은 연결이 끊기지 않고 413 COMMON-007 JSON으로 응답한다`() {
		val (status, body) = upload(11 * 1024 * 1024)

		assertEquals(413, status.value(), body)
		assertEquals(false, JsonPath.read<Boolean>(body, "$.success"))
		assertEquals("COMMON-007", JsonPath.read<String>(body, "$.error.code"))
		// 메시지의 숫자·details·설정(UP3)이 모두 UploadLimits 상수와 같다
		assertEquals("파일 크기는 ${UploadLimits.MAX_FILE_SIZE_MB}MB를 넘을 수 없습니다.", JsonPath.read<String>(body, "$.error.message"))
		assertEquals(UploadLimits.MAX_FILE_SIZE_MB, JsonPath.read<Int>(body, "$.error.details.maxSizeMb"))
	}

	@Test
	fun `UP3 설정의 파일 크기 한도는 UploadLimits 상수와 같다`() {
		assertEquals(DataSize.ofMegabytes(UploadLimits.MAX_FILE_SIZE_MB.toLong()), maxFileSize)
	}
}
