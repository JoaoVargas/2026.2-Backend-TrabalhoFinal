package br.pucpr.alexandria.security

import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Direct unit tests for the two handlers that run at the filter level, before the
 * DispatcherServlet - they can't be reached through [br.pucpr.alexandria.common.GlobalExceptionHandler],
 * so they're exercised here by calling them exactly as Spring Security would. */
class SecurityExceptionHandlersTest {
    private val request = MockHttpServletRequest()

    @Test
    fun `entry point writes a 401 problem detail with the authentication failure message`() {
        val response = MockHttpServletResponse()
        val exception = mock<AuthenticationException> { on { message } doReturn "No credentials found" }

        ProblemDetailEntryPoint().commence(request, response, exception)

        assertEquals(401, response.status)
        assertEquals("application/problem+json", response.contentType)
        val body = ObjectMapper().readTree(response.contentAsString)
        assertEquals(401, body["status"].asInt())
        assertEquals("No credentials found", body["properties"]["errors"]["detail"][0].asString())
    }

    @Test
    fun `entry point falls back to a generic message when the exception has none`() {
        val response = MockHttpServletResponse()
        val exception = mock<AuthenticationException> { on { message } doReturn null }

        ProblemDetailEntryPoint().commence(request, response, exception)

        val body = ObjectMapper().readTree(response.contentAsString)
        assertEquals("Authentication required", body["properties"]["errors"]["detail"][0].asString())
    }

    @Test
    fun `access denied handler writes a 403 problem detail with the denial message`() {
        val response = MockHttpServletResponse()
        val exception = AccessDeniedException("You can only access your own orders")

        ProblemDetailAccessDeniedHandler().handle(request, response, exception)

        assertEquals(403, response.status)
        assertEquals("application/problem+json", response.contentType)
        val body = ObjectMapper().readTree(response.contentAsString)
        assertEquals(403, body["status"].asInt())
        assertTrue(body["properties"]["errors"]["detail"][0].asString().contains("own orders"))
    }

    @Test
    fun `access denied handler falls back to a generic message when the exception has none`() {
        val response = MockHttpServletResponse()
        val exception = mock<AccessDeniedException> { on { message } doReturn null }

        ProblemDetailAccessDeniedHandler().handle(request, response, exception)

        val body = ObjectMapper().readTree(response.contentAsString)
        assertEquals("Access denied", body["properties"]["errors"]["detail"][0].asString())
    }
}
