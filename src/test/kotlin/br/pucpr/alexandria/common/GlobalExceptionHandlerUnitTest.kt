package br.pucpr.alexandria.common

import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Calls [GlobalExceptionHandler] directly for the handful of defensive/reflective branches
 * that no real HTTP request can trigger (Spring MVC never actually throws a bare
 * [TypeMismatchException], and none of this codebase's own exceptions expose a reflective
 * `getStatusCode()`/`getBody()`/`getDetails()`), so they're exercised with purpose-built
 * exception types instead of going through MockMvc. */
class GlobalExceptionHandlerUnitTest {
    private val handler = GlobalExceptionHandler()
    private val request = mock<WebRequest>()

    @Suppress("UNCHECKED_CAST")
    private fun errorsOf(body: Any?): Map<String, List<String>> =
        (body as ProblemDetail).properties?.get("errors") as Map<String, List<String>>

    @Test
    fun `handleTypeMismatch falls back to a generic message for a bare TypeMismatchException`() {
        // handleTypeMismatch keeps the `protected` visibility of the method it overrides, so it's
        // invoked reflectively here - Spring MVC itself only ever throws the more specific
        // MethodArgumentTypeMismatchException, never this base type, through the real HTTP stack.
        val exception = TypeMismatchException("abc", Long::class.java)
        val method = GlobalExceptionHandler::class.java.getDeclaredMethod(
            "handleTypeMismatch",
            TypeMismatchException::class.java,
            HttpHeaders::class.java,
            HttpStatusCode::class.java,
            WebRequest::class.java
        )
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val response = method.invoke(handler, exception, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, request) as ResponseEntity<Any>?

        assertEquals(listOf("Invalid type"), errorsOf(response?.body)["parameter"])
    }

    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    private class AnnotatedButBlankMessageException : RuntimeException()

    @Test
    fun `handleAllExceptions falls back to the status reason phrase when the message is blank`() {
        val response = handler.handleAllExceptions(AnnotatedButBlankMessageException(), request)

        assertEquals(418, response?.statusCode?.value())
        assertEquals("I'm a teapot", (response?.body as ProblemDetail).detail)
    }

    private class ExceptionExposingStatusCode(
        message: String,
        private val code: HttpStatusCode
    ) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): HttpStatusCode = code
    }

    @Test
    fun `handleAllExceptions reads a reflective getStatusCode before falling back to annotations`() {
        val exception = ExceptionExposingStatusCode("custom failure", HttpStatus.CONFLICT)

        val response = handler.handleAllExceptions(exception, request)

        assertEquals(409, response?.statusCode?.value())
        assertEquals("custom failure", (response?.body as ProblemDetail).detail)
    }

    @Test
    fun `handleAllExceptions returns a generic 500 for a reflective 5xx getStatusCode`() {
        val exception = ExceptionExposingStatusCode("db exploded", HttpStatus.SERVICE_UNAVAILABLE)

        val response = handler.handleAllExceptions(exception, request)

        assertEquals(500, response?.statusCode?.value())
        assertEquals("Internal Server Error", (response?.body as ProblemDetail).detail)
    }

    private class ExceptionExposingBody(
        message: String,
        private val code: HttpStatusCode,
        private val problemBody: ProblemDetail
    ) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): HttpStatusCode = code

        @Suppress("unused")
        fun getBody(): ProblemDetail = problemBody
    }

    @Test
    fun `handleAllExceptions copies extra properties from a reflective getBody`() {
        val problemBody = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problemBody.setProperty("reason", "stock depleted")
        val exception = ExceptionExposingBody("conflict", HttpStatus.CONFLICT, problemBody)

        val response = handler.handleAllExceptions(exception, request)

        assertEquals(listOf("stock depleted"), errorsOf(response?.body)["reason"])
    }

    private class ExceptionExposingDetails(
        message: String,
        private val code: HttpStatusCode,
        private val details: Map<String, List<String>>
    ) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): HttpStatusCode = code

        @Suppress("unused")
        fun getDetails(): Map<String, List<String>> = details
    }

    @Test
    fun `handleAllExceptions falls back to a reflective getDetails map when there's no body`() {
        val exception = ExceptionExposingDetails(
            "conflict", HttpStatus.CONFLICT, mapOf("field" to listOf("bad value"))
        )

        val response = handler.handleAllExceptions(exception, request)

        assertEquals(listOf("bad value"), errorsOf(response?.body)["field"])
    }

    private class ExceptionExposingWrongStatusType(message: String) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): String = "not-a-status-code"
    }

    @Test
    fun `extractStatus ignores a reflective getStatusCode that isn't the right type`() {
        val exception = ExceptionExposingWrongStatusType("whatever")

        val response = handler.handleAllExceptions(exception, request)

        assertEquals(500, response?.statusCode?.value())
    }

    private class ExceptionExposingNonProblemDetailBody(
        message: String,
        private val code: HttpStatusCode
    ) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): HttpStatusCode = code

        @Suppress("unused")
        fun getBody(): String = "not a problem detail"
    }

    @Test
    fun `extractDetails ignores a reflective getBody that isn't a ProblemDetail`() {
        val exception = ExceptionExposingNonProblemDetailBody("conflict", HttpStatus.CONFLICT)

        val response = handler.handleAllExceptions(exception, request)

        assertNull((response?.body as ProblemDetail).properties?.get("errors"))
    }

    private class ExceptionExposingNonMapDetails(
        message: String,
        private val code: HttpStatusCode
    ) : RuntimeException(message) {
        @Suppress("unused")
        fun getStatusCode(): HttpStatusCode = code

        @Suppress("unused")
        fun getDetails(): String = "not a map"
    }

    @Test
    fun `extractDetails ignores a reflective getDetails that isn't a map`() {
        val exception = ExceptionExposingNonMapDetails("conflict", HttpStatus.CONFLICT)

        val response = handler.handleAllExceptions(exception, request)

        assertNull((response?.body as ProblemDetail).properties?.get("errors"))
    }

    @Test
    fun `handleAuthenticationException falls back to a generic message when the exception has none`() {
        val exception = mock<AuthenticationException> { on { message } doReturn null }

        val response = handler.handleAuthenticationException(exception)

        assertEquals("Authentication failed", errorsOf(response.body)["detail"]?.get(0))
    }

    @Test
    fun `handleAccessDeniedException falls back to a generic message when the exception has none`() {
        val exception = mock<AccessDeniedException> { on { message } doReturn null }

        val response = handler.handleAccessDeniedException(exception)

        assertEquals("Access denied", errorsOf(response.body)["detail"]?.get(0))
    }
}
