package br.pucpr.alexandria.categories

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

@WebMvcTest(CategoryController::class)
@Import(MethodSecurityTestConfig::class)
class CategoryControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: CategoryService

    @Test
    fun `list returns 200 with every category`() {
        whenever(service.findAll()).thenReturn(listOf(Category(id = 1L, name = "Fantasy")))

        mockMvc.perform(get("/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Fantasy"))
    }

    @Test
    fun `getById returns 404 when the category doesn't exist`() {
        whenever(service.findByIdOrThrow(1L)).thenThrow(NotFoundException("Category 1 not found!"))

        mockMvc.perform(get("/categories/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 201 when an admin creates a category`() {
        whenever(service.insert(any())).thenReturn(Category(id = 1L, name = "Fantasy"))

        mockMvc.perform(
            post("/categories").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Fantasy"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 409 when the category already exists`() {
        whenever(service.insert(any())).thenThrow(ConflictException("Category Fantasy already exists"))

        mockMvc.perform(
            post("/categories").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Fantasy"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `insert returns 401 for an anonymous caller`() {
        mockMvc.perform(
            post("/categories").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Fantasy"}""")
        )
            .andExpect(status().isUnauthorized)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `insert returns 403 for a non-admin caller`() {
        mockMvc.perform(
            post("/categories").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Fantasy"}""")
        )
            .andExpect(status().isForbidden)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteById returns 204 on success`() {
        mockMvc.perform(delete("/categories/1"))
            .andExpect(status().isNoContent)
        verify(service).deleteById(1L)
    }

    @Test
    fun `deleteById returns 401 for an anonymous caller`() {
        mockMvc.perform(delete("/categories/1"))
            .andExpect(status().isUnauthorized)
        verify(service, never()).deleteById(any())
    }
}
