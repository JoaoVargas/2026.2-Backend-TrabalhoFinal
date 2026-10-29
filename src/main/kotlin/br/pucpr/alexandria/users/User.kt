package br.pucpr.alexandria.users

import br.pucpr.alexandria.roles.Role
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "UserTable")
class User(
    @Id @GeneratedValue
    var id: Long? = null,

    @Email
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, length = 100)
    var passwordHash: String,

    @Column(nullable = false)
    var name: String,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "UserRole",
        joinColumns = [JoinColumn(name = "idUser")],
        inverseJoinColumns = [JoinColumn(name = "idRole")],
    )
    var roles: MutableSet<Role> = mutableSetOf()
) : UserDetails {
    override fun getUsername(): String = email
    override fun getPassword(): String = passwordHash
    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

    fun hasRole(name: String) = roles.any { it.name == name.uppercase() }
}
