package br.pucpr.alexandria.categories

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
@Validated
class CategoryController(
    private val service: CategoryService
) {
    @GetMapping
    fun list(): ResponseEntity<List<CategoryResponse>> =
        service.findAll().map { CategoryResponse(it) }.let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(@Min(1) @PathVariable id: Long) =
        CategoryResponse(service.findByIdOrThrow(id)).let { ResponseEntity.ok(it) }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun insert(@Valid @RequestBody request: CategoryRequest) =
        service.insert(request.toCategory())
            .let { CategoryResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteById(@Min(1) @PathVariable id: Long): ResponseEntity<Void> {
        service.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
