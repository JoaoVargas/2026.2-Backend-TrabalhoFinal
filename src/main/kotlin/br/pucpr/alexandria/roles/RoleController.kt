package br.pucpr.alexandria.roles

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roles")
@Validated
class RoleController(
    private val service: RoleService
) {
    @GetMapping
    fun list(): ResponseEntity<List<RoleResponse>> =
        service.findAll()
            .map { RoleResponse(it) }
            .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(
        @Valid @RequestBody role: RoleRequest
    ) = service.insert(role.toRole())
        .let { RoleResponse(it) }
        .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @DeleteMapping("/{name}")
    fun deleteByName(@PathVariable name: String): ResponseEntity<Void> {
        service.deleteByName(name)
        return ResponseEntity.noContent().build()
    }
}
