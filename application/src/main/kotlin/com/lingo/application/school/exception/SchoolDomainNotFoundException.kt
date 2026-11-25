package com.lingo.application.school.exception

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.SchoolErrorType

class SchoolDomainNotFoundException(
	debugMessage: String? = null,
	cause: Throwable? = null,
) : ApplicationException(SchoolErrorType.SCHOOL_DOMAIN_NOT_FOUND, debugMessage, cause)
