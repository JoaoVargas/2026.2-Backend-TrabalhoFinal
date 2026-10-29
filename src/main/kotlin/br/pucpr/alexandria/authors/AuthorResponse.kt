package br.pucpr.alexandria.authors

data class AuthorResponse(
    val id: Long,
    val name: String,
    val bio: String?
) {
    constructor(author: Author) : this(
        id = author.id!!,
        name = author.name,
        bio = author.bio
    )
}
