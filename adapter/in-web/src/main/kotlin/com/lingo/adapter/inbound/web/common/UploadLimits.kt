package com.lingo.adapter.inbound.web.common

// 업로드 파일 하나의 최대 크기. application.yml의 spring.servlet.multipart.max-file-size와 같아야 한다 (UP3가 고정)
object UploadLimits {
	const val MAX_FILE_SIZE_MB = 10
}
