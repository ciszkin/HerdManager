package by.ciszkin.herdmanager.domain.error.mapper

import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.error.AppException

/**
 * Extension function for cleaner error mapping with context.
 * Usage: `runCatching { apiCall() }.mapErrorWithContext("getModels")`
 */
fun <T> Result<T>.mapErrorWithContext(
    operation: String,
    host: String? = null
): Result<T> {
    return if (isSuccess) {
        this
    } else {
        val exception = exceptionOrNull()!!
        val appError = ApiErrorMapper.mapToAppError(
            exception = exception,
            context = ErrorContext(operation = operation, host = host)
        )
        Result.failure(AppException(appError, exception))
    }
}

/**
 * Extension to map exception to AppError without context.
 */
fun Throwable.toAppError(): AppError {
    return ApiErrorMapper.mapToAppError(this)
}
