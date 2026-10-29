package br.pucpr.alexandria.roles

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class RoleRequest(
    @NotNull
    @Pattern(regexp = "[A-Z][A-Z0-9_]+")
    val name: String?,

    @NotBlank
    val description: String?
) {
    fun toRole() = Role(
        id = null,
        name = name!!,
        description = description!!
    )
}
