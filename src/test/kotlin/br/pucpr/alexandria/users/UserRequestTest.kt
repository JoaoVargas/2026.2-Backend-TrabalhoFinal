package br.pucpr.alexandria.users

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRequestTest {
    @Test
    fun `toUser maps every request field onto a new user with no roles`() {
        val request = UserRequest(email = "ana@mail.com", password = "secret123", name = "Ana")

        val user = request.toUser()

        assertEquals("ana@mail.com", request.email)
        assertEquals("secret123", request.password)
        assertEquals("Ana", request.name)
        assertEquals("ana@mail.com", user.email)
        assertEquals("secret123", user.passwordHash)
        assertEquals("Ana", user.name)
        assertNull(user.id)
        assertEquals(emptySet(), user.roles)
    }
}
