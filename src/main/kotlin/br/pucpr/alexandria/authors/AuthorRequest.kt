package br.pucpr.alexandria.authors

import jakarta.validation.constraints.NotBlank

data class AuthorRequest(
    @NotBlank
    var name: String?,

    var bio: String?
) {
    fun toAuthor() = Author(id = null, name = name!!, bio = bio)
}
