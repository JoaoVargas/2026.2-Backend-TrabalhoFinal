package br.pucpr.alexandria.orders

import java.math.BigDecimal
import java.time.Instant

data class OrderItemResponse(
    val id: Long,
    val bookId: Long,
    val bookTitle: String,
    val quantity: Int,
    val unitPrice: BigDecimal
) {
    constructor(item: OrderItem) : this(
        id = item.id!!,
        bookId = item.book.id!!,
        bookTitle = item.book.title,
        quantity = item.quantity,
        unitPrice = item.unitPrice
    )
}

data class OrderResponse(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val createdAt: Instant,
    val items: List<OrderItemResponse>,
    val total: BigDecimal
) {
    constructor(order: Order) : this(
        id = order.id!!,
        userId = order.user.id!!,
        status = order.status,
        createdAt = order.createdAt,
        items = order.items.map { OrderItemResponse(it) },
        total = order.items.fold(BigDecimal.ZERO) { acc, item -> acc + item.unitPrice.multiply(BigDecimal(item.quantity)) }
    )
}
