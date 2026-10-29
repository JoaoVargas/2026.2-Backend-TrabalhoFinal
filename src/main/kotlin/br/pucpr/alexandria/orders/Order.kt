package br.pucpr.alexandria.orders

import br.pucpr.alexandria.users.User
import jakarta.persistence.*
import java.time.Instant

enum class OrderStatus { OPEN, CANCELED }

@Entity
@Table(name = "orders")
class Order(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idUser", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.OPEN,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItem> = mutableListOf()
)
