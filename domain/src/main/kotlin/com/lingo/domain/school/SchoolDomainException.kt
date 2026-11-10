package com.lingo.domain.school

import com.lingo.domain.common.DomainException

sealed class SchoolDomainException(message: String) : DomainException(message)

class InvalidSchoolDomainException(message: String) : SchoolDomainException(message)

class InvalidSchoolNameException(message: String) : SchoolDomainException(message)
