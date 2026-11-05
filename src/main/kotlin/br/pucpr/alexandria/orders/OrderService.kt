package br.pucpr.alexandria.orders

import br.pucpr.alexandria.books.BookService
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.users.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val repository: OrderRepository,
    private val bookService: BookService
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    fun create(user: User): Order {
        val order = repository.save(Order(user = user))
        log.info("Order {} opened for user {}", order.id, user.email)
        return order
    }

    fun findAllFor(user: User): List<Order> =
        if (user.hasRole("ADMIN")) repository.findAll()
        else repository.findByUserId(user.id!!)

    fun findByIdOrThrow(id: Long): Order =
        repository.findByIdOrNull(id) ?: throw NotFoundException("Order $id not found!")

    fun findOwnedOrThrow(id: Long, user: User): Order {
        val order = findByIdOrThrow(id)
        assertOwnerOrAdmin(order, user)
        return order
    }

    fun addItem(orderId: Long, bookId: Long, quantity: Int, user: User): Order {
        val order = findOwnedOrThrow(orderId, user)
        assertOpen(order)
        val book = bookService.findByIdOrThrow(bookId)

        bookService.reserveStock(book, quantity)
        order.items.add(OrderItem(order = order, book = book, quantity = quantity, unitPrice = book.price))
        val saved = repository.save(order)
        log.info("{}x '{}' added to order {}", quantity, book.title, order.id)
        return saved
    }

    fun removeItem(orderId: Long, itemId: Long, user: User): Boolean {
        val order = findOwnedOrThrow(orderId, user)
        assertOpen(order)
        val item = order.items.find { it.id == itemId } ?: return false

        bookService.releaseStock(item.book, item.quantity)
        order.items.remove(item)
        repository.save(order)
        log.info("Item {} removed from order {}", itemId, order.id)
        return true
    }

    fun cancel(orderId: Long, user: User): Order {
        val order = findOwnedOrThrow(orderId, user)
        assertOpen(order)
        order.items.forEach { bookService.releaseStock(it.book, it.quantity) }
        order.status = OrderStatus.CANCELED
        val saved = repository.save(order)
        log.info("Order {} canceled", order.id)
        return saved
    }

    fun checkout(orderId: Long, user: User): Order {
        val order = findOwnedOrThrow(orderId, user)
        assertOpen(order)
        if (order.items.isEmpty()) throw ConflictException("Order ${order.id} has no items!")
        order.status = OrderStatus.COMPLETED
        val saved = repository.save(order)
        log.info("Order {} completed", order.id)
        return saved
    }

    private fun assertOpen(order: Order) {
        if (order.status != OrderStatus.OPEN)
            throw ConflictException("Order ${order.id} is ${order.status} and can no longer be changed!")
    }

    private fun assertOwnerOrAdmin(order: Order, user: User) {
        if (order.user.id != user.id && !user.hasRole("ADMIN")) {
            log.warn("User {} denied access to order {}", user.email, order.id)
            throw AccessDeniedException("You can only access your own orders")
        }
    }
}
