package br.pucpr.alexandria.orders

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.books.Book
import br.pucpr.alexandria.books.BookService
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.users.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderServiceTest {
    private val repository = mock<OrderRepository>()
    private val bookService = mock<BookService>()
    private val service = OrderService(repository, bookService)

    private val customer = User(id = 1L, email = "customer@mail.com", passwordHash = "hash", name = "Ana")
    private val admin = User(
        id = 2L, email = "admin@mail.com", passwordHash = "hash", name = "Root",
        roles = mutableSetOf(Role(id = 1L, name = "ADMIN", description = "d"))
    )
    private val author = Author(id = 1L, name = "Tolkien")
    private val book = Book(
        id = 1L, title = "The Hobbit", isbn = "978-0-345-33968-3",
        price = BigDecimal("39.90"), publishedYear = 1937, stock = 10, author = author
    )

    private fun openOrder(id: Long = 1L, owner: User = customer) = Order(id = id, user = owner)

    @Test
    fun `create opens a new order for the user`() {
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }

        val order = service.create(customer)

        assertEquals(customer, order.user)
        assertEquals(OrderStatus.OPEN, order.status)
    }

    @Test
    fun `findAllFor returns only the customer's orders`() {
        val orders = listOf(openOrder())
        whenever(repository.findByUserId(1L)).thenReturn(orders)

        assertEquals(orders, service.findAllFor(customer))
        verify(repository, never()).findAll()
    }

    @Test
    fun `findAllFor returns every order for an admin`() {
        val orders = listOf(openOrder(), openOrder(id = 2L, owner = admin))
        whenever(repository.findAll()).thenReturn(orders)

        assertEquals(orders, service.findAllFor(admin))
        verify(repository, never()).findByUserId(any())
    }

    @Test
    fun `findOwnedOrThrow throws NotFoundException when the order doesn't exist`() {
        whenever(repository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.findOwnedOrThrow(1L, customer) }
    }

    @Test
    fun `findOwnedOrThrow denies access to orders belonging to another user`() {
        val order = openOrder(owner = customer)
        val stranger = User(id = 99L, email = "stranger@mail.com", passwordHash = "hash", name = "Stranger")
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        assertThrows<AccessDeniedException> { service.findOwnedOrThrow(1L, stranger) }
    }

    @Test
    fun `findOwnedOrThrow allows an admin to access any order`() {
        val order = openOrder(owner = customer)
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        assertEquals(order, service.findOwnedOrThrow(1L, admin))
    }

    @Test
    fun `addItem reserves stock and appends the item`() {
        val order = openOrder()
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))
        whenever(bookService.findByIdOrThrow(1L)).thenReturn(book)
        whenever(repository.save(order)).thenReturn(order)

        val result = service.addItem(1L, 1L, 2, customer)

        assertEquals(1, result.items.size)
        assertEquals(book, result.items[0].book)
        verify(bookService).reserveStock(book, 2)
    }

    @Test
    fun `addItem throws ConflictException when the order isn't open`() {
        val order = openOrder().apply { status = OrderStatus.COMPLETED }
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        assertThrows<ConflictException> { service.addItem(1L, 1L, 1, customer) }
        verify(bookService, never()).reserveStock(any(), any())
    }

    @Test
    fun `removeItem releases stock and drops the item`() {
        val order = openOrder()
        val item = OrderItem(id = 10L, order = order, book = book, quantity = 2, unitPrice = book.price)
        order.items.add(item)
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        val removed = service.removeItem(1L, 10L, customer)

        assertTrue(removed)
        assertTrue(order.items.isEmpty())
        verify(bookService).releaseStock(book, 2)
    }

    @Test
    fun `removeItem returns false when the item doesn't exist`() {
        val order = openOrder()
        order.items.add(OrderItem(id = 10L, order = order, book = book, quantity = 2, unitPrice = book.price))
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        val removed = service.removeItem(1L, 999L, customer)

        assertFalse(removed)
        verify(bookService, never()).releaseStock(any(), any())
    }

    @Test
    fun `cancel releases stock for every item and marks the order canceled`() {
        val order = openOrder()
        val item = OrderItem(id = 10L, order = order, book = book, quantity = 3, unitPrice = book.price)
        order.items.add(item)
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))
        whenever(repository.save(order)).thenReturn(order)

        val result = service.cancel(1L, customer)

        assertEquals(OrderStatus.CANCELED, result.status)
        verify(bookService).releaseStock(book, 3)
    }

    @Test
    fun `cancel throws ConflictException when the order isn't open`() {
        val order = openOrder().apply { status = OrderStatus.CANCELED }
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        assertThrows<ConflictException> { service.cancel(1L, customer) }
    }

    @Test
    fun `checkout completes an order that has items`() {
        val order = openOrder()
        order.items.add(OrderItem(id = 10L, order = order, book = book, quantity = 1, unitPrice = book.price))
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))
        whenever(repository.save(order)).thenReturn(order)

        val result = service.checkout(1L, customer)

        assertEquals(OrderStatus.COMPLETED, result.status)
    }

    @Test
    fun `checkout throws ConflictException for an order with no items`() {
        val order = openOrder()
        whenever(repository.findById(1L)).thenReturn(Optional.of(order))

        assertThrows<ConflictException> { service.checkout(1L, customer) }
    }
}
