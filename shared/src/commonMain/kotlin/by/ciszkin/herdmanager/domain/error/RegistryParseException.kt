package by.ciszkin.herdmanager.domain.error

/**
 * Thrown when the Ollama registry page was fetched but could not be parsed
 * into model cards — typically because ollama.com changed its markup or
 * served a challenge/error page. Mapped to [RegistryParseError].
 */
class RegistryParseException(
    val reason: String,
    cause: Throwable? = null
) : Exception("Failed to parse the Ollama registry page: $reason", cause)