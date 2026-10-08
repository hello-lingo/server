package com.lingo.domain.user

sealed class DomainException(message: String) : RuntimeException(message)

class InvalidEmailException(message: String) : DomainException(message)

class InvalidPasswordException(message: String) : DomainException(message)

class InvalidNameException(message: String) : DomainException(message)
