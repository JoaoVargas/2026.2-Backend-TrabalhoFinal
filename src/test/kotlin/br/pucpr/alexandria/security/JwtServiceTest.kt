package br.pucpr.alexandria.security

import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.users.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtServiceTest {
    private val service = JwtService(
        secret = "test-secret-key-for-alexandria-unit-tests-only-32bytes+",
        expirationMinutes = 60
    )

    private fun user() = User(
        id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana",
        roles = mutableSetOf(Role(id = 1L, name = "ADMIN", description = "d"))
    )

    @Test
    fun `generateToken produces a token whose subject is the user's email`() {
        val token = service.generateToken(user())

        assertEquals("ana@mail.com", service.extractEmail(token))
    }

    @Test
    fun `isValid is true for a freshly generated token`() {
        val token = service.generateToken(user())

        assertTrue(service.isValid(token))
    }

    @Test
    fun `isValid is false for a tampered token`() {
        val token = service.generateToken(user())

        assertFalse(service.isValid(token.dropLast(2) + "xx"))
    }

    @Test
    fun `isValid is false for garbage input`() {
        assertFalse(service.isValid("not-a-jwt"))
    }

    @Test
    fun `a token signed with a different secret is rejected`() {
        val otherService = JwtService(secret = "another-completely-different-secret-key-32bytes!", expirationMinutes = 60)
        val token = otherService.generateToken(user())

        assertFalse(service.isValid(token))
    }

    @Test
    fun `expirationSeconds converts minutes to seconds`() {
        assertEquals(3600, service.expirationSeconds())
    }
}
