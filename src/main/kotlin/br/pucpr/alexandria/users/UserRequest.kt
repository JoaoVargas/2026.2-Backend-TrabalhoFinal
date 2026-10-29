package br.pucpr.alexandria.users

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UserRequest(
    @NotNull
    @Email
    var email: String?,

    @NotBlank
    @Size(min = 4)
    var password: String?,

    @NotBlank
    var name: String?
) {
    fun toUser() = User(
        id = null,
        email = email!!,
        passwordHash = password!!,
        name = name!!
    )
}
