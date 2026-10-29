package br.pucpr.alexandria.users

data class UserResponse(
    var id: Long,
    var email: String,
    var name: String,
    var roles: List<String>
) {
    constructor(user: User) : this(
        id = user.id!!,
        email = user.email,
        name = user.name,
        roles = user.roles.map { it.name }
    )
}
