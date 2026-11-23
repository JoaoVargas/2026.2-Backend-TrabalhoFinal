package br.pucpr.alexandria.auth

import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.security.JwtService
import br.pucpr.alexandria.users.User
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
class AuthControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var authenticationManager: AuthenticationManager

    @MockitoBean
    lateinit var jwtService: JwtService

    private val user = User(
        id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana",
        roles = mutableSetOf(Role(id = 1L, name = "ADMIN", description = "d"))
    )

    @Test
    fun `login returns a token for valid credentials`() {
        whenever(authenticationManager.authenticate(any()))
            .thenReturn(UsernamePasswordAuthenticationToken(user, null, user.authorities))
        whenever(jwtService.generateToken(user)).thenReturn("signed.jwt.token")
        whenever(jwtService.expirationSeconds()).thenReturn(3600L)

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@mail.com","password":"secret"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("ana@mail.com"))
    }

    @Test
    fun `login returns 401 for bad credentials`() {
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Bad credentials"))

        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"ana@mail.com","password":"wrong"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login returns 422 for a malformed request`() {
        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","password":""}""")
        )
            .andExpect(status().isUnprocessableContent)
    }
}
