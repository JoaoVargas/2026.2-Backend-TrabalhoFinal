package br.pucpr.alexandria.security

import br.pucpr.alexandria.users.User
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertNull
import kotlin.test.assertSame

class JwtAuthenticationFilterTest {
    private val jwtService = mock<JwtService>()
    private val userDetailsService = mock<UserDetailsService>()
    private val filter = JwtAuthenticationFilter(jwtService, userDetailsService)
    private val chain = mock<FilterChain>()

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `requests without an Authorization header pass through unauthenticated`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `headers without the Bearer prefix are ignored`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Basic dXNlcjpwYXNz") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtService, never()).extractEmail(any())
    }

    @Test
    fun `a valid token authenticates the matching user`() {
        val user = User(id = 1L, email = "ana@mail.com", passwordHash = "hash", name = "Ana")
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer good.token") }
        val response = MockHttpServletResponse()
        whenever(jwtService.extractEmail("good.token")).thenReturn("ana@mail.com")
        whenever(jwtService.isValid("good.token")).thenReturn(true)
        whenever(userDetailsService.loadUserByUsername("ana@mail.com")).thenReturn(user)

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertSame(user, authentication?.principal)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `a token that fails validation is ignored`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer bad.token") }
        val response = MockHttpServletResponse()
        whenever(jwtService.extractEmail("bad.token")).thenReturn("ana@mail.com")
        whenever(jwtService.isValid("bad.token")).thenReturn(false)

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(userDetailsService, never()).loadUserByUsername(any())
    }

    @Test
    fun `an unparsable token is ignored`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer garbage") }
        val response = MockHttpServletResponse()
        whenever(jwtService.extractEmail("garbage")).thenReturn(null)

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtService, never()).isValid(any())
    }

    @Test
    fun `a valid token for a user that no longer exists is ignored`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer good.token") }
        val response = MockHttpServletResponse()
        whenever(jwtService.extractEmail("good.token")).thenReturn("ghost@mail.com")
        whenever(jwtService.isValid("good.token")).thenReturn(true)
        whenever(userDetailsService.loadUserByUsername("ghost@mail.com")).thenThrow(UsernameNotFoundException("gone"))

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `an already authenticated context is left untouched`() {
        val existing = UsernamePasswordAuthenticationToken("already-set", null, emptyList())
        SecurityContextHolder.getContext().authentication = existing
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer good.token") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, chain)

        assertSame(existing, SecurityContextHolder.getContext().authentication)
        verify(jwtService, never()).extractEmail(any())
    }
}
