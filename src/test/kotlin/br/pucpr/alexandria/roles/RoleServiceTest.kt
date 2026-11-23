package br.pucpr.alexandria.roles

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.users.User
import br.pucpr.alexandria.users.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import kotlin.test.assertEquals

class RoleServiceTest {
    private val repository = mock<RoleRepository>()
    private val userRepository = mock<UserRepository>()
    private val service = RoleService(repository, userRepository)

    @Test
    fun `findAll returns roles sorted by name`() {
        val roles = listOf(Role(id = 1L, name = "ADMIN", description = "d"))
        whenever(repository.findAll(any<Sort>())).thenReturn(roles)

        val result = service.findAll()

        assertEquals(roles, result)
        verify(repository).findAll(Sort.by("name"))
    }

    @Test
    fun `insert uppercases the role name before saving`() {
        val request = Role(id = null, name = "editor", description = "can edit")
        whenever(repository.findByName("EDITOR")).thenReturn(null)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.insert(request)

        assertEquals("EDITOR", result.name)
        verify(repository).save(request)
    }

    @Test
    fun `insert throws ConflictException when the role already exists`() {
        val request = Role(id = null, name = "admin", description = "d")
        whenever(repository.findByName("ADMIN")).thenReturn(Role(id = 1L, name = "ADMIN", description = "d"))

        assertThrows<ConflictException> { service.insert(request) }
        verify(repository, never()).save(any())
    }

    @Test
    fun `deleteByName removes a role that no user holds`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")
        whenever(repository.findByName("ADMIN")).thenReturn(role)
        whenever(userRepository.findByRole("ADMIN")).thenReturn(emptyList())

        service.deleteByName("admin")

        verify(repository).delete(role)
    }

    @Test
    fun `deleteByName throws ConflictException when the role is still assigned`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")
        whenever(repository.findByName("ADMIN")).thenReturn(role)
        whenever(userRepository.findByRole("ADMIN")).thenReturn(listOf(mock<User>()))

        assertThrows<ConflictException> { service.deleteByName("admin") }
        verify(repository, never()).delete(any())
    }

    @Test
    fun `deleteByName throws NotFoundException for an unknown role`() {
        whenever(repository.findByName("GHOST")).thenReturn(null)

        assertThrows<NotFoundException> { service.deleteByName("ghost") }
    }
}
