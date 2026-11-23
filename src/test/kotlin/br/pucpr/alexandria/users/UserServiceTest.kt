package br.pucpr.alexandria.users

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.common.SortDir
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.roles.RoleRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest {
    private val repository = mock<UserRepository>()
    private val roleRepository = mock<RoleRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val service = UserService(repository, roleRepository, passwordEncoder)

    @Test
    fun `insert hashes the password and saves a new user`() {
        val user = User(email = "a@b.com", passwordHash = "plain", name = "Ana")
        whenever(repository.findByEmail("a@b.com")).thenReturn(null)
        whenever(passwordEncoder.encode("plain")).thenReturn("hashed")
        whenever(repository.save(user)).thenReturn(user)

        val result = service.insert(user)

        assertEquals("hashed", result.passwordHash)
        verify(repository).save(user)
    }

    @Test
    fun `insert throws ConflictException when the email is already registered`() {
        val existing = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana")
        val user = User(email = "a@b.com", passwordHash = "plain", name = "Ana")
        whenever(repository.findByEmail("a@b.com")).thenReturn(existing)

        assertThrows<ConflictException> { service.insert(user) }
        verify(repository, never()).save(any())
    }

    @Test
    fun `update changes the name when it differs`() {
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Old")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))
        whenever(repository.save(user)).thenReturn(user)

        val result = service.update(1L, "New")

        assertEquals("New", result?.name)
        verify(repository).save(user)
    }

    @Test
    fun `update returns null and skips saving when the name is unchanged`() {
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Same")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))

        val result = service.update(1L, "Same")

        assertNull(result)
        verify(repository, never()).save(any())
    }

    @Test
    fun `findByRole uppercases the role and delegates to the repository`() {
        val admins = listOf(User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana"))
        whenever(repository.findByRole("ADMIN")).thenReturn(admins)

        val result = service.findByRole("admin")

        assertEquals(admins, result)
    }

    @Test
    fun `findByIdOrThrow throws NotFoundException when the user doesn't exist`() {
        whenever(repository.findById(42L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.findByIdOrThrow(42L) }
    }

    @Test
    fun `findAll sorts ascending by name`() {
        whenever(repository.findAll(any<Sort>())).thenReturn(emptyList())

        service.findAll(SortDir.ASC)

        verify(repository).findAll(Sort.by("name"))
    }

    @Test
    fun `findAll sorts descending by name`() {
        whenever(repository.findAll(any<Sort>())).thenReturn(emptyList())

        service.findAll(SortDir.DESC)

        verify(repository).findAll(Sort.by("name").descending())
    }

    @Test
    fun `deleteById removes the user`() {
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))

        service.deleteById(1L)

        verify(repository).delete(user)
    }

    @Test
    fun `addRole grants a role the user doesn't have yet`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))
        whenever(roleRepository.findByName("ADMIN")).thenReturn(role)

        val granted = service.addRole(1L, "admin")

        assertTrue(granted)
        assertTrue(user.roles.contains(role))
        verify(repository).save(user)
    }

    @Test
    fun `addRole is idempotent when the user already has the role`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana", roles = mutableSetOf(role))
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))

        val granted = service.addRole(1L, "ADMIN")

        assertFalse(granted)
        verify(repository, never()).save(any())
    }

    @Test
    fun `addRole throws NotFoundException for an unknown role`() {
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))
        whenever(roleRepository.findByName("GHOST")).thenReturn(null)

        assertThrows<NotFoundException> { service.addRole(1L, "ghost") }
    }

    @Test
    fun `removeRole revokes a role the user has`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana", roles = mutableSetOf(role))
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))

        val removed = service.removeRole(1L, "admin")

        assertTrue(removed)
        assertTrue(user.roles.isEmpty())
        verify(repository).save(user)
    }

    @Test
    fun `removeRole returns false when the user doesn't have the role`() {
        val user = User(id = 1L, email = "a@b.com", passwordHash = "hash", name = "Ana")
        whenever(repository.findById(1L)).thenReturn(Optional.of(user))

        val removed = service.removeRole(1L, "ADMIN")

        assertFalse(removed)
        verify(repository, never()).save(any())
    }
}
