package com.lingo.application.school.exception

import com.lingo.application.common.error.ApplicationException
import com.lingo.application.common.error.SchoolErrorType

class DuplicateSchoolNameException(
	debugMessage: String? = null,
	cause: Throwable? = null,
) : ApplicationException(SchoolErrorType.DUPLICATE_SCHOOL_NAME, debugMessage, cause)
