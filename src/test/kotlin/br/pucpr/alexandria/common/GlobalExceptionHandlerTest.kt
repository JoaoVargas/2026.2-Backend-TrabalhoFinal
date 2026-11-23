package br.pucpr.alexandria.common

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.authors.AuthorController
import br.pucpr.alexandria.authors.AuthorService
import br.pucpr.alexandria.books.BookController
import br.pucpr.alexandria.books.BookService
import br.pucpr.alexandria.security.MethodSecurityTestConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@WebMvcTest(controllers = [AuthorController::class, BookController::class])
@Import(MethodSecurityTestConfig::class)
class GlobalExceptionHandlerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var authorService: AuthorService

    @MockitoBean
    lateinit var bookService: BookService

    @Test
    fun `syntactically broken JSON is reported as a malformed request`() {
        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Tolkien" """)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid JSON request"))
            .andExpect(jsonPath("$.errors.body[0]").value("Malformed JSON request"))
    }

    @Test
    fun `a value that can't be coerced into its target type is reported per field`() {
        mockMvc.perform(
            post("/books").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"Foo","isbn":"123","price":"not-a-number",
                        "publishedYear":2000,"stock":1,"authorId":1}"""
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.price[0]").value("Invalid type. Expected BigDecimal"))
    }

    @Test
    fun `a structurally wrong JSON value falls back to a generic field format error`() {
        mockMvc.perform(
            post("/books").contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"Foo","isbn":"123","price":10.0,
                        "publishedYear":2000,"stock":1,"authorId":{}}"""
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.authorId[0]").value("Invalid field format"))
    }

    @Test
    fun `a path variable that isn't a number is reported as an invalid URL parameter`() {
        mockMvc.perform(get("/authors/not-a-number"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid URL parameter"))
            .andExpect(jsonPath("$.errors.id[0]").value("Invalid type. Expected long"))
    }

    @Test
    fun `a path variable that violates a bean constraint fails validation`() {
        mockMvc.perform(get("/authors/0"))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.detail").value("Validation failed"))
    }

    @Test
    fun `an unexpected exception is reported as a generic 500 without leaking details`() {
        whenever(authorService.findByIdOrThrow(1L)).thenThrow(IllegalStateException("db connection pool exhausted"))

        mockMvc.perform(get("/authors/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.detail").value("Internal Server Error"))
    }

    @Test
    fun `a custom not-found exception still carries the original message`() {
        whenever(authorService.findByIdOrThrow(99L)).thenThrow(NotFoundException("Author 99 not found!"))

        mockMvc.perform(get("/authors/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Author 99 not found!"))
    }

    @Test
    fun `a request body that fails field validation reports every offending field`() {
        whenever(authorService.insert(any())).thenReturn(Author(id = 1L, name = "x"))

        mockMvc.perform(
            post("/authors").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.detail").value("Invalid request body"))
            .andExpect(jsonPath("$.errors.name").isArray())
    }
}
