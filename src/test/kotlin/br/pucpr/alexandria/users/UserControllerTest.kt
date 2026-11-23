package br.pucpr.alexandria.users

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.SortDir
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.security.WebSecurityTestConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(UserController::class)
@Import(WebSecurityTestConfig::class)
class UserControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var userService: UserService

    private val owner = User(id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana")
    private val admin = User(
        id = 2L, email = "admin@mail.com", passwordHash = "hash", name = "Root",
        roles = mutableSetOf(Role(id = 1L, name = "ADMIN", description = "d"))
    )
    private val stranger = User(id = 3L, email = "bob@mail.com", passwordHash = "hash", name = "Bob")

    @Test
    fun `list is open to anonymous callers`() {
        whenever(userService.findAll(any())).thenReturn(listOf(owner))

        mockMvc.perform(get("/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].email").value("ana@mail.com"))
    }

    @Test
    fun `list honors an explicit sortDir`() {
        whenever(userService.findAll(SortDir.DESC)).thenReturn(listOf(owner))

        mockMvc.perform(get("/users").param("sortDir", "DESC"))
            .andExpect(status().isOk)
        verify(userService, never()).findByRole(any())
    }

    @Test
    fun `list filters by role instead of sorting`() {
        whenever(userService.findByRole("ADMIN")).thenReturn(listOf(admin))

        mockMvc.perform(get("/users").param("role", "ADMIN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].email").value("admin@mail.com"))
        verify(userService, never()).findAll(any())
    }

    @Test
    fun `insert returns 201 for a new registration`() {
        whenever(userService.insert(any())).thenReturn(owner)

        mockMvc.perform(
            post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@mail.com","password":"secret","name":"Ana"}""")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `insert returns 409 when the email is already registered`() {
        whenever(userService.insert(any())).thenThrow(ConflictException("User already exists"))

        mockMvc.perform(
            post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@mail.com","password":"secret","name":"Ana"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `update allows a user to change their own name`() {
        whenever(userService.update(1L, "New Name")).thenReturn(owner.apply { name = "New Name" })

        mockMvc.perform(
            patch("/users/1").with(user(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Name"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
    }

    @Test
    fun `update returns 204 when the name is unchanged`() {
        whenever(userService.update(1L, "Ana")).thenReturn(null)

        mockMvc.perform(
            patch("/users/1").with(user(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Ana"}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `update allows an admin to change someone else's name`() {
        whenever(userService.update(1L, "New Name")).thenReturn(owner.apply { name = "New Name" })

        mockMvc.perform(
            patch("/users/1").with(user(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Name"}""")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `update returns 403 when a user tries to edit someone else's profile`() {
        mockMvc.perform(
            patch("/users/1").with(user(stranger)).contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Hacked"}""")
        )
            .andExpect(status().isForbidden)
        verify(userService, never()).update(any(), any())
    }

    @Test
    fun `deleteById returns 204 for an admin`() {
        mockMvc.perform(delete("/users/1").with(user(admin)))
            .andExpect(status().isNoContent)
        verify(userService).deleteById(1L)
    }

    @Test
    fun `deleteById returns 403 for a non-admin`() {
        mockMvc.perform(delete("/users/1").with(user(owner)))
            .andExpect(status().isForbidden)
        verify(userService, never()).deleteById(any())
    }

    @Test
    fun `grantRole returns 200 when newly granted`() {
        whenever(userService.addRole(1L, "ADMIN")).thenReturn(true)

        mockMvc.perform(put("/users/1/roles/ADMIN").with(user(admin)))
            .andExpect(status().isOk)
    }

    @Test
    fun `grantRole returns 403 for a non-admin caller`() {
        mockMvc.perform(put("/users/1/roles/ADMIN").with(user(owner)))
            .andExpect(status().isForbidden)
        verify(userService, never()).addRole(any(), any())
    }

    @Test
    fun `grantRole returns 204 when the user already has the role`() {
        whenever(userService.addRole(1L, "ADMIN")).thenReturn(false)

        mockMvc.perform(put("/users/1/roles/ADMIN").with(user(admin)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `revokeRole returns 204 when successfully removed`() {
        whenever(userService.removeRole(1L, "ADMIN")).thenReturn(true)

        mockMvc.perform(delete("/users/1/roles/ADMIN").with(user(admin)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `revokeRole returns 404 when the user doesn't have the role`() {
        whenever(userService.removeRole(1L, "ADMIN")).thenReturn(false)

        mockMvc.perform(delete("/users/1/roles/ADMIN").with(user(admin)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `revokeRole returns 403 for a non-admin caller`() {
        mockMvc.perform(delete("/users/1/roles/ADMIN").with(user(owner)))
            .andExpect(status().isForbidden)
        verify(userService, never()).removeRole(any(), any())
    }
}
