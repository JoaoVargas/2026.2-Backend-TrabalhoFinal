package br.pucpr.alexandria.users

import jakarta.validation.constraints.NotBlank

data class PatchUserRequest(
    @NotBlank
    var name: String?
)
