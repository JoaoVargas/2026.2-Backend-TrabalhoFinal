package br.pucpr.alexandria.books

import br.pucpr.alexandria.authors.Author
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
import java.math.BigDecimal

@WebMvcTest(BookController::class)
@Import(MethodSecurityTestConfig::class)
class BookControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: BookService

    private val author = Author(id = 1L, name = "Tolkien")
    private fun book() = Book(
        id = 1L, title = "The Hobbit", isbn = "978-0-345-33968-3",
        price = BigDecimal("39.90"), publishedYear = 1937, stock = 10, author = author
    )

    @Test
    fun `search is open to anonymous callers and forwards query params`() {
        whenever(service.search("hobbit", null, null, null, null, "title", "ASC")).thenReturn(listOf(book()))

        mockMvc.perform(get("/books").param("title", "hobbit"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("The Hobbit"))
    }

    @Test
    fun `search returns 400 for an unsupported sort field`() {
        whenever(service.search(null, null, null, null, null, "unknown", "ASC"))
            .thenThrow(InvalidSortFieldException("unknown"))

        mockMvc.perform(get("/books").param("sortBy", "unknown"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getById returns 404 when the book doesn't exist`() {
        whenever(service.findByIdOrThrow(99L)).thenThrow(NotFoundException("Book 99 not found!"))

        mockMvc.perform(get("/books/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `insert returns 201 when an admin creates a book`() {
        whenever(service.insert(any())).thenReturn(book())

        mockMvc.perform(
            post("/books").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"The Hobbit","isbn":"978-0-345-33968-3","price":39.90,
                        "publishedYear":1937,"stock":10,"authorId":1}"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("The Hobbit"))
    }

    @Test
    fun `insert returns 401 for an anonymous caller`() {
        mockMvc.perform(
            post("/books").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"The Hobbit","isbn":"978-0-345-33968-3","price":39.90,
                        "publishedYear":1937,"stock":10,"authorId":1}"""
                )
        )
            .andExpect(status().isUnauthorized)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["CUSTOMER"])
    fun `insert returns 403 for a non-admin caller`() {
        mockMvc.perform(
            post("/books").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"The Hobbit","isbn":"978-0-345-33968-3","price":39.90,
                        "publishedYear":1937,"stock":10,"authorId":1}"""
                )
        )
            .andExpect(status().isForbidden)
        verify(service, never()).insert(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `update returns 200 with the updated book`() {
        whenever(service.update(eq(1L), any())).thenReturn(book())

        mockMvc.perform(
            put("/books/1").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"The Hobbit","isbn":"978-0-345-33968-3","price":39.90,
                        "publishedYear":1937,"stock":10,"authorId":1}"""
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteById returns 204 on success`() {
        mockMvc.perform(delete("/books/1"))
            .andExpect(status().isNoContent)
        verify(service).deleteById(1L)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `addCategory returns 201 when the category was newly attached`() {
        whenever(service.addCategory(1L, 2L)).thenReturn(true)

        mockMvc.perform(post("/books/1/categories/2"))
            .andExpect(status().isCreated)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `addCategory returns 204 when the category was already attached`() {
        whenever(service.addCategory(1L, 2L)).thenReturn(false)

        mockMvc.perform(post("/books/1/categories/2"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `addCategory returns 401 for an anonymous caller`() {
        mockMvc.perform(post("/books/1/categories/2"))
            .andExpect(status().isUnauthorized)
        verify(service, never()).addCategory(any(), any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `removeCategory returns 204 when removed`() {
        whenever(service.removeCategory(1L, 2L)).thenReturn(true)

        mockMvc.perform(delete("/books/1/categories/2"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `removeCategory returns 404 when the category wasn't attached`() {
        whenever(service.removeCategory(1L, 2L)).thenReturn(false)

        mockMvc.perform(delete("/books/1/categories/2"))
            .andExpect(status().isNotFound)
    }
}
