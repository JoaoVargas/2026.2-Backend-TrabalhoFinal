package br.pucpr.alexandria

import br.pucpr.alexandria.authors.AuthorRepository
import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.categories.CategoryRepository
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.roles.RoleRepository
import br.pucpr.alexandria.users.User
import br.pucpr.alexandria.users.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder

class BootStrapperTest {
    private val roleRepository = mock<RoleRepository>()
    private val userRepository = mock<UserRepository>()
    private val authorRepository = mock<AuthorRepository>()
    private val categoryRepository = mock<CategoryRepository>()
    private val bookRepository = mock<BookRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val bootStrapper = BootStrapper(
        roleRepository, userRepository, authorRepository, categoryRepository, bookRepository, passwordEncoder
    )
    private val event = mock<ContextRefreshedEvent>()

    @Test
    fun `seeds roles, the admin user and the catalog on a fresh database`() {
        whenever(roleRepository.findByName(any())).thenReturn(null)
        whenever(roleRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(userRepository.findByRole("ADMIN")).thenReturn(emptyList())
        whenever(passwordEncoder.encode(any())).thenReturn("hashed")
        whenever(authorRepository.count()).thenReturn(0L)
        whenever(authorRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(categoryRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(bookRepository.save(any())).thenAnswer { it.arguments[0] }

        bootStrapper.onApplicationEvent(event)

        verify(roleRepository, times(2)).save(any())
        verify(userRepository).save(argThat<User> { email == "admin@alexandria.com" && roles.any { it.name == "ADMIN" } })
        verify(authorRepository, times(2)).save(any())
        verify(categoryRepository, times(3)).save(any())
        verify(bookRepository, times(3)).save(any())
    }

    @Test
    fun `does nothing when roles, the admin user and the catalog already exist`() {
        val adminRole = Role(id = 1L, name = "ADMIN", description = "System administrator")
        whenever(roleRepository.findByName("ADMIN")).thenReturn(adminRole)
        whenever(roleRepository.findByName("CUSTOMER")).thenReturn(Role(id = 2L, name = "CUSTOMER", description = "d"))
        whenever(userRepository.findByRole("ADMIN")).thenReturn(listOf(mock<User>()))
        whenever(authorRepository.count()).thenReturn(5L)

        bootStrapper.onApplicationEvent(event)

        verify(roleRepository, never()).save(any())
        verify(userRepository, never()).save(any())
        verify(authorRepository, never()).save(any())
        verify(categoryRepository, never()).save(any())
        verify(bookRepository, never()).save(any())
    }
}
