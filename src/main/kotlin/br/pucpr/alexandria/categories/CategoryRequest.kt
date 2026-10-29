package br.pucpr.alexandria.categories

import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @NotBlank
    var name: String?
) {
    fun toCategory() = Category(id = null, name = name!!)
}
