package by.ciszkin.herdmanager.domain.error.mapper

import by.ciszkin.herdmanager.domain.error.AppError
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
import kotlinx.serialization.SerializationException
import kotlinx.io.IOException

/**
 * Centralized error mapper for API exceptions.
 * Maps common exceptions across all endpoints to typed AppError.
 */
object ApiErrorMapper {

    /**
     * Maps an exception to an AppError with contextual information.
     */
    fun mapToAppError(
        exception: Throwable,
        context: ErrorContext = ErrorContext()
    ): AppError {
        return when (exception) {

            // AppException wrapper - extract the actual AppError
            is AppException -> exception.appError

            // HTTP status code errors from Ollama API
            is OllamaApiException -> HttpError(
                cause = exception,
                statusCode = exception.statusCode,
                endpoint = exception.endpoint
            )

            // Registry page could not be parsed (markup mismatch / challenge)
            is RegistryParseException -> RegistryParseError(cause = exception)

            // Timeout errors
            is HttpRequestTimeoutException -> TimeoutError(
                cause = exception,
                operation = context.operation ?: "request",
                timeoutMs = 0
            )

            // Parsing/serialization errors
            is SerializationException,
            is NoTransformationFoundException -> ParsingError(
                cause = exception,
                contentType = "response",
                expectedFormat = "JSON"
            )

            // Network connectivity errors
            is IOException -> ConnectionError(
                cause = exception,
                host = context.host ?: "server"
            )

            // Fallback for unexpected errors
            else -> UnexpectedError(
                cause = exception,
                context = context.operation ?: "unknown"
            )
        }
    }
}
