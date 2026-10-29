package br.pucpr.alexandria.users

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.common.SortDir
import br.pucpr.alexandria.roles.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val repository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun insert(user: User): User {
        if (repository.findByEmail(user.email) != null) {
            throw ConflictException("User already exists")
        }
        user.passwordHash = passwordEncoder.encode(user.passwordHash)!!
        val saved = repository.save(user)
        log.info("User {} registered", saved.email)
        return saved
    }

    fun update(id: Long, name: String): User? {
        val user = findByIdOrThrow(id)
        if (user.name == name) return null
        user.name = name
        return repository.save(user)
    }

    fun findAll(dir: SortDir): List<User> =
        if (dir == SortDir.ASC) repository.findAll(Sort.by("name"))
        else repository.findAll(Sort.by("name").descending())

    fun findByRole(role: String): List<User> = repository.findByRole(role.uppercase())

    fun findByIdOrNull(id: Long): User? = repository.findByIdOrNull(id)

    fun findByIdOrThrow(id: Long): User =
        findByIdOrNull(id) ?: throw NotFoundException("User $id not found!")

    fun deleteById(id: Long) {
        val user = findByIdOrThrow(id)
        repository.delete(user)
        log.info("User {} deleted", user.email)
    }

    fun addRole(id: Long, roleName: String): Boolean {
        val upperRole = roleName.uppercase()
        val user = findByIdOrThrow(id)
        if (user.hasRole(upperRole)) return false
        val role = roleRepository.findByName(upperRole)
            ?: throw NotFoundException("Role $upperRole not found!")

        user.roles.add(role)
        repository.save(user)
        log.info("Role {} granted to user {}", upperRole, user.email)
        return true
    }

    fun removeRole(id: Long, roleName: String): Boolean {
        val upperRole = roleName.uppercase()
        val user = findByIdOrThrow(id)
        val removed = user.roles.removeAll { it.name == upperRole }
        if (removed) {
            repository.save(user)
            log.info("Role {} revoked from user {}", upperRole, user.email)
        }
        return removed
    }
}
