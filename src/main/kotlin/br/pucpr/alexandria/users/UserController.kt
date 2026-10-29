package br.pucpr.alexandria.users

import br.pucpr.alexandria.common.SortDir
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
@Validated
class UserController(
    private val userService: UserService
) {
    @GetMapping
    fun list(
        @RequestParam sortDir: String?,
        @RequestParam role: String?
    ): ResponseEntity<List<UserResponse>> {
        val users = if (role != null) userService.findByRole(role)
        else userService.findAll(SortDir.findOrThrow(sortDir ?: "ASC"))

        return users.map { UserResponse(it) }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/{id}")
    fun getById(@Min(1) @PathVariable id: Long) =
        UserResponse(userService.findByIdOrThrow(id))
            .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(@Valid @RequestBody user: UserRequest) =
        userService.insert(user.toUser())
            .let { UserResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PatchMapping("/{id}")
    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody user: PatchUserRequest
    ) = userService.update(id, user.name!!)
        ?.let { UserResponse(it) }
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.noContent().build()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteById(@Min(1) @PathVariable id: Long): ResponseEntity<Void> {
        userService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/roles/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun grantRole(
        @Min(1) @PathVariable id: Long,
        @PathVariable name: String
    ): ResponseEntity<Void> =
        if (userService.addRole(id, name)) ResponseEntity.ok().build()
        else ResponseEntity.noContent().build()

    @DeleteMapping("/{id}/roles/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun revokeRole(
        @Min(1) @PathVariable id: Long,
        @PathVariable name: String
    ): ResponseEntity<Void> =
        if (userService.removeRole(id, name)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
