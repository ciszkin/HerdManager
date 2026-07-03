package by.ciszkin.herdmanager.domain.error.mapper

/**
 * Contextual information for error mapping.
 * Provides additional details to create more specific error messages.
 */
data class ErrorContext(
    /** The operation being performed (e.g., "getModels", "pullModel") */
    val operation: String? = null,

    /** The host/server being contacted (e.g., "localhost:11434") */
    val host: String? = null
)
