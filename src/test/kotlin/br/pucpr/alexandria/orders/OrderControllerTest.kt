package br.pucpr.alexandria.orders

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.books.Book
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.security.WebSecurityTestConfig
import br.pucpr.alexandria.users.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@WebMvcTest(OrderController::class)
@Import(WebSecurityTestConfig::class)
class OrderControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: OrderService

    private val customer = User(id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana")
    private val author = Author(id = 1L, name = "Tolkien")
    private val book = Book(
        id = 1L, title = "The Hobbit", isbn = "978-0-345-33968-3",
        price = BigDecimal("39.90"), publishedYear = 1937, stock = 10, author = author
    )

    @Test
    fun `list returns 200 with the caller's orders`() {
        val order = Order(id = 1L, user = customer)
        whenever(service.findAllFor(any())).thenReturn(listOf(order))

        mockMvc.perform(get("/orders").with(user(customer)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
    }

    @Test
    fun `getById returns 403 when the order belongs to someone else`() {
        whenever(service.findOwnedOrThrow(1L, customer)).thenThrow(AccessDeniedException("You can only access your own orders"))

        mockMvc.perform(get("/orders/1").with(user(customer)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `create returns 201 with a new open order`() {
        val order = Order(id = 1L, user = customer)
        whenever(service.create(any())).thenReturn(order)

        mockMvc.perform(post("/orders").with(user(customer)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    fun `addItem returns 201 when the item is added`() {
        val order = Order(id = 1L, user = customer)
        order.items.add(OrderItem(id = 1L, order = order, book = book, quantity = 2, unitPrice = book.price))
        whenever(service.addItem(1L, 1L, 2, customer)).thenReturn(order)

        mockMvc.perform(
            post("/orders/1/items").with(user(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":1,"quantity":2}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items[0].quantity").value(2))
    }

    @Test
    fun `addItem returns 409 when stock is insufficient`() {
        whenever(service.addItem(1L, 1L, 99, customer))
            .thenThrow(ConflictException("Only 10 unit(s) of 'The Hobbit' left in stock"))

        mockMvc.perform(
            post("/orders/1/items").with(user(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"bookId":1,"quantity":99}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `removeItem returns 204 when removed`() {
        whenever(service.removeItem(1L, 2L, customer)).thenReturn(true)

        mockMvc.perform(delete("/orders/1/items/2").with(user(customer)))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `removeItem returns 404 when the item doesn't exist`() {
        whenever(service.removeItem(1L, 2L, customer)).thenReturn(false)

        mockMvc.perform(delete("/orders/1/items/2").with(user(customer)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `checkout returns 200 with the completed order`() {
        val order = Order(id = 1L, user = customer, status = OrderStatus.COMPLETED)
        whenever(service.checkout(1L, customer)).thenReturn(order)

        mockMvc.perform(post("/orders/1/checkout").with(user(customer)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `cancel returns 200 with the canceled order`() {
        val order = Order(id = 1L, user = customer, status = OrderStatus.CANCELED)
        whenever(service.cancel(1L, customer)).thenReturn(order)

        mockMvc.perform(delete("/orders/1").with(user(customer)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELED"))
    }

    @Test
    fun `an admin can list every order`() {
        val admin = User(
            id = 2L, email = "admin@mail.com", passwordHash = "hash", name = "Root",
            roles = mutableSetOf(Role(id = 1L, name = "ADMIN", description = "d"))
        )
        whenever(service.findAllFor(admin)).thenReturn(listOf(Order(id = 1L, user = customer), Order(id = 2L, user = admin)))

        mockMvc.perform(get("/orders").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
