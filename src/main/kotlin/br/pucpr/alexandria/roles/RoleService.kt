package br.pucpr.alexandria.roles

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.users.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class RoleService(
    private val repository: RoleRepository,
    private val userRepository: UserRepository
) {
    fun insert(role: Role): Role {
        role.name = role.name.uppercase()
        if (repository.findByName(role.name) != null) {
            throw ConflictException("Role ${role.name} already exists")
        }
        return repository.save(role)
    }

    fun findAll(): List<Role> = repository.findAll(Sort.by("name"))

    fun deleteByName(name: String) {
        val role = repository.findByName(name.uppercase())
            ?: throw NotFoundException("Role $name not found!")
        if (userRepository.findByRole(role.name).isNotEmpty()) {
            throw ConflictException("Role ${role.name} is still assigned to users")
        }
        repository.delete(role)
    }
}
