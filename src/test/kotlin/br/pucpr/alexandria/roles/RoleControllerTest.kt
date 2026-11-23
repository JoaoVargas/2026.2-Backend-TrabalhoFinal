package br.pucpr.alexandria.roles

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.security.MethodSecurityTestConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(RoleController::class)
@Import(MethodSecurityTestConfig::class)
class RoleControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: RoleService

    @Test
    fun `list returns 200 and is open to anonymous callers`() {
        whenever(service.findAll()).thenReturn(listOf(Role(id = 1L, name = "ADMIN", description = "d")))

        mockMvc.perform(get("/roles"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("ADMIN"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 201 when an admin creates a role`() {
        whenever(service.insert(any())).thenReturn(Role(id = 1L, name = "EDITOR", description = "can edit"))

        mockMvc.perform(
            post("/roles").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"EDITOR","description":"can edit"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("EDITOR"))
    }

    @Test
    fun `insert returns 401 for an anonymous caller`() {
        mockMvc.perform(
            post("/roles").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"EDITOR","description":"can edit"}""")
        )
            .andExpect(status().isUnauthorized)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `insert returns 403 for a non-admin caller`() {
        mockMvc.perform(
            post("/roles").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"EDITOR","description":"can edit"}""")
        )
            .andExpect(status().isForbidden)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 409 when the role already exists`() {
        whenever(service.insert(any())).thenThrow(ConflictException("Role ADMIN already exists"))

        mockMvc.perform(
            post("/roles").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"ADMIN","description":"d"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteByName returns 204 on success`() {
        mockMvc.perform(delete("/roles/EDITOR"))
            .andExpect(status().isNoContent)
        verify(service).deleteByName("EDITOR")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteByName returns 409 when the role is still assigned to users`() {
        doThrow(ConflictException("Role ADMIN is still assigned to users")).whenever(service).deleteByName("ADMIN")

        mockMvc.perform(delete("/roles/ADMIN"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `deleteByName returns 401 for an anonymous caller`() {
        mockMvc.perform(delete("/roles/ADMIN"))
            .andExpect(status().isUnauthorized)
        verify(service, never()).deleteByName(any())
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `deleteByName returns 403 for a non-admin caller`() {
        mockMvc.perform(delete("/roles/ADMIN"))
            .andExpect(status().isForbidden)
        verify(service, never()).deleteByName(any())
    }
}
