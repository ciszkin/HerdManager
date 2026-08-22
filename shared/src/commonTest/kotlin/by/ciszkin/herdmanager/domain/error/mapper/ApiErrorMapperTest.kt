package by.ciszkin.herdmanager.domain.error.mapper

import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.HttpError
import by.ciszkin.herdmanager.domain.error.OllamaApiException
import by.ciszkin.herdmanager.domain.error.ParsingError
import by.ciszkin.herdmanager.domain.error.RegistryParseError
import by.ciszkin.herdmanager.domain.error.RegistryParseException
import by.ciszkin.herdmanager.domain.error.TimeoutError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiErrorMapperTest {

    @Test
    fun `AppException is unwrapped`() {
        val inner = HttpError(cause = null, statusCode = 500, endpoint = "/test")
        val exception = AppException(inner)
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<HttpError>(result)
        assertEquals(500, result.statusCode)
    }

    @Test
    fun `OllamaApiException maps to HttpError`() {
        val exception = OllamaApiException(404, "/api/delete")
        val result = ApiErrorMapper.mapToAppError(exception, ErrorContext(operation = "deleteModel"))
        assertIs<HttpError>(result)
        assertEquals(404, result.statusCode)
        assertEquals("/api/delete", result.endpoint)
        assertTrue(result.isRetryable.not())
    }

    @Test
    fun `OllamaApiException 500 is retryable`() {
        val exception = OllamaApiException(500, "/api/tags")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<HttpError>(result)
        assertEquals(500, result.statusCode)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `OllamaApiException 429 is retryable`() {
        val exception = OllamaApiException(429, "/api/tags")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<HttpError>(result)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `HttpRequestTimeoutException maps to TimeoutError`() {
        val exception = mockk<HttpRequestTimeoutException>()
        val result = ApiErrorMapper.mapToAppError(exception, ErrorContext(operation = "getModels"))
        assertIs<TimeoutError>(result)
        assertEquals("getModels", result.operation)
    }

    @Test
    fun `SerializationException maps to ParsingError`() {
        val exception = SerializationException("Unexpected JSON token")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<ParsingError>(result)
        assertEquals("response", result.contentType)
        assertEquals("JSON", result.expectedFormat)
    }

    @Test
    fun `NoTransformationFoundException maps to ParsingError`() {
        val exception = mockk<NoTransformationFoundException>()
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<ParsingError>(result)
        assertEquals("response", result.contentType)
        assertEquals("JSON", result.expectedFormat)
    }

    @Test
    fun `OllamaApiException with server message still maps to HttpError at mapper level`() {
        val exception = OllamaApiException(404, "/api/pull", serverMessage = "model 'x' not found")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<HttpError>(result)
        assertEquals(404, result.statusCode)
        assertEquals("/api/pull", result.endpoint)
    }

    @Test
    fun `RegistryParseException maps to RegistryParseError`() {
        val exception = RegistryParseException("no model cards parsed")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<RegistryParseError>(result)
        assertTrue(result.isRetryable.not())
    }

    @Test
    fun `IOException maps to ConnectionError`() {
        val exception = IOException("Connection refused")
        val result = ApiErrorMapper.mapToAppError(exception, ErrorContext(host = "localhost:11434"))
        assertIs<ConnectionError>(result)
        assertEquals("localhost:11434", result.host)
    }

    @Test
    fun `Unknown exception maps to UnexpectedError`() {
        val exception = IllegalStateException("Something weird")
        val result = ApiErrorMapper.mapToAppError(exception, ErrorContext(operation = "testOp"))
        assertIs<UnexpectedError>(result)
        assertEquals("testOp", result.context)
    }

    @Test
    fun `Default context uses unknown operation`() {
        val exception = RuntimeException("oops")
        val result = ApiErrorMapper.mapToAppError(exception)
        assertIs<UnexpectedError>(result)
        assertEquals("unknown", result.context)
    }
}
