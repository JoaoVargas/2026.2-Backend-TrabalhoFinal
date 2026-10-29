package br.pucpr.alexandria.orders

import br.pucpr.alexandria.users.User
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
@Validated
class OrderController(
    private val service: OrderService
) {
    @GetMapping
    fun list(@AuthenticationPrincipal user: User): ResponseEntity<List<OrderResponse>> =
        service.findAllFor(user).map { OrderResponse(it) }.let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(
        @Min(1) @PathVariable id: Long,
        @AuthenticationPrincipal user: User
    ) = OrderResponse(service.findOwnedOrThrow(id, user)).let { ResponseEntity.ok(it) }

    @PostMapping
    fun create(@AuthenticationPrincipal user: User) =
        service.create(user)
            .let { OrderResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PostMapping("/{id}/items")
    fun addItem(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody request: OrderItemRequest,
        @AuthenticationPrincipal user: User
    ) = OrderResponse(service.addItem(id, request.bookId!!, request.quantity!!, user))
        .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @DeleteMapping("/{id}/items/{itemId}")
    fun removeItem(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable itemId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> =
        if (service.removeItem(id, itemId, user)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @DeleteMapping("/{id}")
    fun cancel(
        @Min(1) @PathVariable id: Long,
        @AuthenticationPrincipal user: User
    ) = OrderResponse(service.cancel(id, user)).let { ResponseEntity.ok(it) }
}
