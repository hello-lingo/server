package com.lingo.application.common.error

// 사용자용 문장은 받지 않는다 (응답 문장은 ErrorCode에서만). debugMessage는 로그에만 남는 개발자용 메모이며 개인정보를 넣지 않는다.
abstract class ApplicationException(
	val errorType: ErrorType,
	val debugMessage: String? = null,
	cause: Throwable? = null,
) : RuntimeException(debugMessage ?: errorType.toString(), cause) {
	/** 응답과 로그에 함께 나가는 값. 사용자가 봐도 되는 값만 넣는다 */
	open val details: Map<String, Any?>? = null
}
