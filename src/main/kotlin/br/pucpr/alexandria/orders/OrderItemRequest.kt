package br.pucpr.alexandria.orders

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class OrderItemRequest(
    @NotNull
    @Min(1)
    var bookId: Long?,

    @NotNull
    @Min(1)
    var quantity: Int?
)
