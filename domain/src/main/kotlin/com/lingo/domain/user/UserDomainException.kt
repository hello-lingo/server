package com.lingo.domain.user

import com.lingo.domain.common.DomainException

sealed class UserDomainException(message: String) : DomainException(message)

class InvalidEmailException(message: String) : UserDomainException(message)

class InvalidPasswordException(message: String) : UserDomainException(message)

class InvalidNameException(message: String) : UserDomainException(message)
