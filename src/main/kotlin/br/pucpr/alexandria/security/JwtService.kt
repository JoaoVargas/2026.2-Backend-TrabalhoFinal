package br.pucpr.alexandria.security

import br.pucpr.alexandria.users.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiration-minutes}") private val expirationMinutes: Long
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.email)
            .claim("uid", user.id)
            .claim("roles", user.roles.map { it.name })
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    fun expirationSeconds(): Long = expirationMinutes * 60

    fun extractEmail(token: String): String? =
        runCatching { parseClaims(token).payload.subject }.getOrNull()

    fun isValid(token: String): Boolean =
        runCatching { parseClaims(token) }.isSuccess

    private fun parseClaims(token: String) =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
}
