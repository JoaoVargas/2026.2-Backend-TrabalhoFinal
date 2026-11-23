package br.pucpr.alexandria.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
    @NotNull
    @Email
    var email: String?,

    @NotBlank
    var password: String?
)
