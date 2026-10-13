package com.lingo.application.user.exception

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.UserErrorType

class InvalidCredentialsException(
	debugMessage: String? = null,
	cause: Throwable? = null,
) : ApplicationException(UserErrorType.INVALID_CREDENTIALS, debugMessage, cause)
