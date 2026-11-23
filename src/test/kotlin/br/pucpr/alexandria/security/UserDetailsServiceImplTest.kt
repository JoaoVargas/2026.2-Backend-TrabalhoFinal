package br.pucpr.alexandria.security

import br.pucpr.alexandria.users.User
import br.pucpr.alexandria.users.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertEquals

class UserDetailsServiceImplTest {
    private val repository = mock<UserRepository>()
    private val service = UserDetailsServiceImpl(repository)

    @Test
    fun `loadUserByUsername returns the matching user`() {
        val user = User(id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana")
        whenever(repository.findByEmail("ana@mail.com")).thenReturn(user)

        assertEquals(user, service.loadUserByUsername("ana@mail.com"))
    }

    @Test
    fun `loadUserByUsername throws UsernameNotFoundException when there's no match`() {
        whenever(repository.findByEmail("ghost@mail.com")).thenReturn(null)

        assertThrows<UsernameNotFoundException> { service.loadUserByUsername("ghost@mail.com") }
    }
}
