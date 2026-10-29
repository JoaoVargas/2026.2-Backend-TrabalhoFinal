package br.pucpr.alexandria.authors

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/authors")
@Validated
class AuthorController(
    private val service: AuthorService
) {
    @GetMapping
    fun list(): ResponseEntity<List<AuthorResponse>> =
        service.findAll().map { AuthorResponse(it) }.let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(@Min(1) @PathVariable id: Long) =
        AuthorResponse(service.findByIdOrThrow(id)).let { ResponseEntity.ok(it) }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun insert(@Valid @RequestBody request: AuthorRequest) =
        service.insert(request.toAuthor())
            .let { AuthorResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody request: AuthorRequest
    ) = AuthorResponse(service.update(id, request)).let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteById(@Min(1) @PathVariable id: Long): ResponseEntity<Void> {
        service.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
