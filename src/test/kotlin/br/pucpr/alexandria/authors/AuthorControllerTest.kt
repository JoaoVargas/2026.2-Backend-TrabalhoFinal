package br.pucpr.alexandria.authors

import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
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

@WebMvcTest(AuthorController::class)
@Import(MethodSecurityTestConfig::class)
class AuthorControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: AuthorService

    @Test
    fun `list returns 200 with every author`() {
        whenever(service.findAll()).thenReturn(listOf(Author(id = 1L, name = "Tolkien", bio = "writer")))

        mockMvc.perform(get("/authors"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Tolkien"))
    }

    @Test
    fun `getById returns 200 when the author exists`() {
        whenever(service.findByIdOrThrow(1L)).thenReturn(Author(id = 1L, name = "Tolkien"))

        mockMvc.perform(get("/authors/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Tolkien"))
    }

    @Test
    fun `getById returns 404 when the author doesn't exist`() {
        whenever(service.findByIdOrThrow(99L)).thenThrow(NotFoundException("Author 99 not found!"))

        mockMvc.perform(get("/authors/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 201 when an admin creates an author`() {
        whenever(service.insert(any())).thenReturn(Author(id = 1L, name = "Tolkien", bio = "writer"))

        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Tolkien","bio":"writer"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `insert returns 401 for an anonymous caller`() {
        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Tolkien","bio":"writer"}""")
        )
            .andExpect(status().isUnauthorized)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `insert returns 403 for a non-admin caller`() {
        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Tolkien","bio":"writer"}""")
        )
            .andExpect(status().isForbidden)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 422 for an invalid request body`() {
        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isUnprocessableContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `update returns 200 with the updated author`() {
        whenever(service.update(eq(1L), any())).thenReturn(Author(id = 1L, name = "New Name"))

        mockMvc.perform(
            put("/authors/1").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"New Name"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteById returns 204 on success`() {
        mockMvc.perform(delete("/authors/1"))
            .andExpect(status().isNoContent)
        verify(service).deleteById(1L)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteById returns 409 when the author still has books`() {
        doThrow(ConflictException("Author Tolkien still has books and cannot be deleted")).whenever(service).deleteById(1L)

        mockMvc.perform(delete("/authors/1"))
            .andExpect(status().isConflict)
    }
}
