package br.pucpr.alexandria.books

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/books")
@Validated
class BookController(
    private val service: BookService
) {
    /** Filtering and sorting via query params, e.g.
     * GET /books?title=hobbit&categoryId=2&minPrice=10&maxPrice=100&sortBy=price&sortDir=DESC */
    @GetMapping
    fun search(
        @RequestParam title: String?,
        @RequestParam authorId: Long?,
        @RequestParam categoryId: Long?,
        @RequestParam minPrice: BigDecimal?,
        @RequestParam maxPrice: BigDecimal?,
        @RequestParam(defaultValue = "title") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDir: String
    ): ResponseEntity<List<BookResponse>> =
        service.search(title, authorId, categoryId, minPrice, maxPrice, sortBy, sortDir)
            .map { BookResponse(it) }
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(@Min(1) @PathVariable id: Long) =
        BookResponse(service.findByIdOrThrow(id)).let { ResponseEntity.ok(it) }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun insert(@Valid @RequestBody request: BookRequest) =
        service.insert(request)
            .let { BookResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody request: BookRequest
    ) = BookResponse(service.update(id, request)).let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteById(@Min(1) @PathVariable id: Long): ResponseEntity<Void> {
        service.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun addCategory(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable categoryId: Long
    ): ResponseEntity<Void> =
        if (service.addCategory(id, categoryId)) ResponseEntity.status(HttpStatus.CREATED).build()
        else ResponseEntity.noContent().build()

    @DeleteMapping("/{id}/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun removeCategory(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable categoryId: Long
    ): ResponseEntity<Void> =
        if (service.removeCategory(id, categoryId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
