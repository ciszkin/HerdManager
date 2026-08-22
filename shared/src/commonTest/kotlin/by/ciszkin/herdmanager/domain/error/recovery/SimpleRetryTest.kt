package by.ciszkin.herdmanager.domain.error.recovery

import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.HttpError
import by.ciszkin.herdmanager.domain.error.ParsingError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleRetryTest {

    @Test
    fun `returns success on the first attempt`() = runTest {
        val result = retryOnFailure { Result.success(42) }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `retries retryable errors until success`() = runTest {
        var attempts = 0
        val operation: suspend () -> Result<Int> = {
            attempts++
            if (attempts < 3) {
                Result.failure(AppException(ConnectionError(cause = null, host = "localhost")))
            } else {
                Result.success(42)
            }
        }

        val result = retryOnFailure(config = RetryConfig(maxAttempts = 3, delayMs = 10), operation = operation)

        assertTrue(result.isSuccess)
        assertEquals(3, attempts)
    }

    @Test
    fun `gives up after maxAttempts on persistent retryable failure`() = runTest {
        var attempts = 0
        val operation: suspend () -> Result<Int> = {
            attempts++
            Result.failure(AppException(HttpError(cause = null, statusCode = 500, endpoint = "/x")))
        }

        val result = retryOnFailure(config = RetryConfig(maxAttempts = 3, delayMs = 10), operation = operation)

        assertTrue(result.isFailure)
        assertEquals(3, attempts)
        assertIs<AppException>(result.exceptionOrNull())
        assertIs<HttpError>((result.exceptionOrNull() as AppException).appError)
    }

    @Test
    fun `does not retry non-retryable errors`() = runTest {
        var attempts = 0
        val operation: suspend () -> Result<Int> = {
            attempts++
            Result.failure(AppException(UnexpectedError(cause = null, context = "op")))
        }

        val result = retryOnFailure(config = RetryConfig(maxAttempts = 3, delayMs = 10), operation = operation)

        assertTrue(result.isFailure)
        assertEquals(1, attempts)
        assertIs<AppException>(result.exceptionOrNull())
    }

    @Test
    fun `does not surface a new error for a plain exception`() = runTest {
        val operation: suspend () -> Result<Int> = {
            Result.failure(IllegalStateException("boom"))
        }

        val result = retryOnFailure(config = RetryConfig(maxAttempts = 2, delayMs = 10), operation = operation)

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `waits the configured delay between retries`() = runTest {
        var attempts = 0
        val operation: suspend () -> Result<Int> = {
            attempts++
            if (attempts == 1) {
                Result.failure(AppException(ConnectionError(cause = null, host = "localhost")))
            } else {
                Result.success(1)
            }
        }

        val job = launch {
            retryOnFailure(config = RetryConfig(maxAttempts = 2, delayMs = 100), operation = operation)
        }

        advanceTimeBy(50)
        runCurrent()
        assertEquals(1, attempts, "Second attempt must not start before the delay elapses")

        advanceTimeBy(50)
        runCurrent()
        assertEquals(2, attempts, "Second attempt starts once the delay has elapsed")

        job.join()
    }
}