package com.lingo.application.user.exception

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.UserErrorType

class DuplicateEmailException(
	debugMessage: String? = null,
	cause: Throwable? = null,
) : ApplicationException(UserErrorType.DUPLICATE_EMAIL, debugMessage, cause)
