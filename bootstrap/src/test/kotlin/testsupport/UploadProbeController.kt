package testsupport

import com.lingo.adapter.inbound.web.common.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// com.lingo 밖 패키지라 컴포넌트 스캔에 잡히지 않는다. (필요한 테스트에서만 @Import로 등록)
@RestController
class UploadProbeController {

	@PostMapping("/api/v1/upload-probe", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun upload(@RequestPart("file") file: MultipartFile): ApiResponse<Map<String, Long>> =
		ApiResponse.success(mapOf("size" to file.size))
}
