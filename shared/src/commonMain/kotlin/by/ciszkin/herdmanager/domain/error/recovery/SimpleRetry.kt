package by.ciszkin.herdmanager.domain.error.recovery

import by.ciszkin.herdmanager.domain.error.mapper.toAppError
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration for retry behavior.
 */
data class RetryConfig(
    /** Maximum number of attempts (including first attempt) */
    val maxAttempts: Int = 3,

    /** Delay between attempts in milliseconds */
    val delayMs: Long = 1000
)

/**
 * Retries an operation on failure with configured delay.
 * Only retries if the error is retryable (based on AppError.isRetryable).
 *
 * @param config Retry configuration
 * @param operation The operation to retry
 * @return Result.Success on first success, Result.Failure if all attempts fail
 */
suspend fun <T> retryOnFailure(
    config: RetryConfig = RetryConfig(),
    operation: suspend () -> Result<T>
): Result<T> {
    var lastError: Throwable? = null

    repeat(config.maxAttempts) { attempt ->
        val result = operation()
        if (result.isSuccess) {
            return result
        } else {
            lastError = result.exceptionOrNull()

            // Check if error is retryable
            val appError = lastError?.toAppError()
            val shouldRetry = appError?.isRetryable != false

            // Don't delay after last attempt or if not retryable
            if (attempt < config.maxAttempts - 1 && shouldRetry) {
                delay(config.delayMs.milliseconds)
            } else if (!shouldRetry) {
                // Don't retry non-retryable errors
                return result
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    return Result.failure(lastError as Throwable)
}
