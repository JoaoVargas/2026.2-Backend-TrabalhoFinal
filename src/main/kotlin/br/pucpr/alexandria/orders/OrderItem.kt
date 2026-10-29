package br.pucpr.alexandria.orders

import br.pucpr.alexandria.books.Book
import jakarta.persistence.*
import java.math.BigDecimal

/** Join entity between Order and Book: a many-to-many relationship carrying
 * extra attributes (quantity and the price charged at the time of purchase). */
@Entity
class OrderItem(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idOrder", nullable = false)
    var order: Order,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idBook", nullable = false)
    var book: Book,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var unitPrice: BigDecimal
)
