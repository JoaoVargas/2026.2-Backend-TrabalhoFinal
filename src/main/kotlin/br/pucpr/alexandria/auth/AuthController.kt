package br.pucpr.alexandria.auth

import br.pucpr.alexandria.security.JwtService
import br.pucpr.alexandria.users.User
import br.pucpr.alexandria.users.UserResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val user = authentication.principal as User
        val token = jwtService.generateToken(user)
        return ResponseEntity.ok(
            LoginResponse(token = token, expiresIn = jwtService.expirationSeconds(), user = UserResponse(user))
        )
    }
}
