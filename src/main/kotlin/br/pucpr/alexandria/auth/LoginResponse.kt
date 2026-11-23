package br.pucpr.alexandria.auth

import br.pucpr.alexandria.users.UserResponse

data class LoginResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)
