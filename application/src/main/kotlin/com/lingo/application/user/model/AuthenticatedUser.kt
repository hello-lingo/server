package com.lingo.application.user.model

import com.lingo.domain.user.Role

data class AuthenticatedUser(val userId: Long, val email: String, val role: Role)
