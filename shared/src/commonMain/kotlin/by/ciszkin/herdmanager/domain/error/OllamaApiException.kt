package by.ciszkin.herdmanager.domain.error

/**
 * Thrown when an API request receives a non-successful HTTP status code.
 * Carries the server-provided error message when available.
 * Mapped to [HttpError] by [mapper.ApiErrorMapper].
 */
class OllamaApiException(
    val statusCode: Int,
    val endpoint: String,
    val serverMessage: String? = null,
    cause: Throwable? = null
) : Exception(serverMessage ?: "HTTP $statusCode for $endpoint", cause)