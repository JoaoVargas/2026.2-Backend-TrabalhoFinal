package br.pucpr.alexandria.roles

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoleTest {
    @Test
    fun `a role equals itself`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")

        assertTrue(role == role)
    }

    @Test
    fun `roles with the same id are equal`() {
        val a = Role(id = 1L, name = "ADMIN", description = "d")
        val b = Role(id = 1L, name = "OTHER_NAME", description = "other")

        assertTrue(a == b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `roles with different ids are not equal`() {
        val a = Role(id = 1L, name = "ADMIN", description = "d")
        val b = Role(id = 2L, name = "ADMIN", description = "d")

        assertFalse(a == b)
    }

    @Test
    fun `a role is never equal to null`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")

        assertFalse(role.equals(null))
    }

    @Test
    fun `a role is never equal to an instance of another class`() {
        val role = Role(id = 1L, name = "ADMIN", description = "d")

        assertFalse(role.equals("ADMIN"))
    }
}
