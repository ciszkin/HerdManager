package by.ciszkin.herdmanager.domain.error

/**
 * Exception wrapper for AppError to allow it to be used in Result<T>.
 */
class AppException(
    val appError: AppError,
    override val cause: Throwable? = appError.cause
) : Exception(appError.technicalMessage, cause) {
    override fun toString(): String {
        return "AppException: ${appError.technicalMessage}"
    }
}

/**
 * Base interface for all application errors.
 * Provides structured error handling with user-friendly messages.
 */
sealed interface AppError {
    /** The underlying cause, if any */
    val cause: Throwable?

    /** Technical message for debugging/logging */
    val technicalMessage: String

    /** Whether this error is retryable (default: false) */
    val isRetryable: Boolean get() = false
}

/**
 * Network-related errors that are typically retryable.
 */
sealed interface NetworkError : AppError {
    /** Whether this error is retryable (default: true) */
    override val isRetryable: Boolean get() = true
}

/**
 * Request timed out.
 */
data class TimeoutError(
    override val cause: Throwable?,
    val operation: String,
    val timeoutMs: Long
) : NetworkError {
    override val technicalMessage = "Operation '$operation' timed out after ${timeoutMs}ms"
}

/**
 * Failed to connect to host.
 */
data class ConnectionError(
    override val cause: Throwable?,
    val host: String
) : NetworkError {
    override val technicalMessage = "Failed to connect to $host"
}

/**
 * HTTP error with status code.
 */
data class HttpError(
    override val cause: Throwable?,
    val statusCode: Int,
    val endpoint: String
) : AppError {
    override val technicalMessage = "HTTP $statusCode for $endpoint"

    /** Server errors are retryable, client errors are not */
    override val isRetryable: Boolean = statusCode in 500..599 || statusCode == 429
}

/**
 * Server-side errors (Ollama-specific).
 */
sealed interface ServerError : AppError {
    override val isRetryable: Boolean get() = true  // Server errors are retryable
}

/**
 * Ollama server is not available.
 */
data class OllamaUnavailableError(
    override val cause: Throwable?
) : ServerError {
    override val technicalMessage = "Ollama server is not responding"
}

/**
 * Requested model not found on server.
 */
data class ModelNotFoundError(
    override val cause: Throwable?,
    val modelName: String
) : ServerError {
    override val technicalMessage = "Model '$modelName' not found on server"
    override val isRetryable = false  // Don't retry if model doesn't exist
}

/**
 * Data processing errors (parsing, validation).
 */
sealed interface DataError : AppError {
    override val isRetryable: Boolean get() = false  // Data errors are not retryable
}

/**
 * Failed to parse response data.
 */
data class ParsingError(
    override val cause: Throwable?,
    val contentType: String,
    val expectedFormat: String
) : DataError {
    override val technicalMessage = "Failed to parse $contentType, expected $expectedFormat"
}

/**
 * Data validation failed.
 */
data class ValidationError(
    override val cause: Throwable?,
    val field: String,
    val constraint: String
) : DataError {
    override val technicalMessage = "Validation failed for '$field': $constraint"
}

/**
 * Failed to parse the Ollama registry page (markup mismatch, challenge page,
 * or otherwise unparseable HTML). Usually indicates the site changed its
 * markup — surfaced as an error instead of a silent empty list.
 */
data class RegistryParseError(
    override val cause: Throwable?
) : DataError {
    override val technicalMessage = "Failed to parse the Ollama registry page"
}

/**
 * Unexpected/unknown error - usually indicates a bug.
 */
data class UnexpectedError(
    override val cause: Throwable?,
    val context: String
) : AppError {
    override val technicalMessage = "Unexpected error in $context: ${cause?.message}"
}
