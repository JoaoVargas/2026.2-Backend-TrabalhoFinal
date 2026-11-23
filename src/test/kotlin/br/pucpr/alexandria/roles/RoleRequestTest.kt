package br.pucpr.alexandria.roles

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoleRequestTest {
    @Test
    fun `toRole maps every request field onto a new role`() {
        val request = RoleRequest(name = "EDITOR", description = "can edit")

        val role = request.toRole()

        assertEquals("EDITOR", request.name)
        assertEquals("can edit", request.description)
        assertEquals("EDITOR", role.name)
        assertEquals("can edit", role.description)
        assertNull(role.id)
    }
}
