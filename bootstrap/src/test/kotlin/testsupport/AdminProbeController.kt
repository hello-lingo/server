package testsupport

import com.lingo.adapter.inbound.web.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// com.lingo 밖 패키지라 컴포넌트 스캔에 잡히지 않는다. (필요한 테스트에서만 @Import로 등록)
@RestController
class AdminProbeController {

	@GetMapping("/api/v1/admin/probe")
	fun probe(): ApiResponse<String> = ApiResponse.success("ok")
}
