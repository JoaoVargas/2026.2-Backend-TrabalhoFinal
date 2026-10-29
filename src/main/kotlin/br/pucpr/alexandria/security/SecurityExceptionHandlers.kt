package br.pucpr.alexandria.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.ObjectMapper

/** Entry point for requests that lack valid credentials (filter chain runs before the DispatcherServlet,
 * so this can't be handled by GlobalExceptionHandler). */
class ProblemDetailEntryPoint : AuthenticationEntryPoint {
    private val mapper = ObjectMapper()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Unauthorized")
        problemDetail.setProperty("errors", mapOf("detail" to listOf(authException.message ?: "Authentication required")))
        write(response, HttpStatus.UNAUTHORIZED, problemDetail)
    }

    private fun write(response: HttpServletResponse, status: HttpStatus, body: ProblemDetail) {
        response.status = status.value()
        response.contentType = "application/problem+json"
        response.writer.write(mapper.writeValueAsString(body))
    }
}

/** Access denied for requests blocked by the authorizeHttpRequests DSL (filter level, not @PreAuthorize). */
class ProblemDetailAccessDeniedHandler : AccessDeniedHandler {
    private val mapper = ObjectMapper()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden")
        problemDetail.setProperty("errors", mapOf("detail" to listOf(accessDeniedException.message ?: "Access denied")))
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/problem+json"
        response.writer.write(mapper.writeValueAsString(problemDetail))
    }
}
