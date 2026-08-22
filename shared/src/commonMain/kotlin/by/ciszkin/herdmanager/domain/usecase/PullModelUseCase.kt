package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.ModelNotFoundError
import by.ciszkin.herdmanager.domain.error.OllamaApiException
import by.ciszkin.herdmanager.domain.error.OllamaUnavailableError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.error.mapper.toAppError
import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Maps Ollama server error messages to specific AppError types.
 * This provides better error handling and user messages for pull operations.
 */
private fun mapPullError(errorMessage: String, modelName: String): AppError {
    val lowerMessage = errorMessage.lowercase()

    return when {
        // Model not found errors
        lowerMessage.contains("model") && lowerMessage.contains("not found") ->
            ModelNotFoundError(cause = null, modelName = modelName)

        // Ollama not running / unavailable errors
        lowerMessage.contains("ollama") && lowerMessage.contains("not running") ||
        lowerMessage.contains("ollama") && lowerMessage.contains("not responding") ||
        lowerMessage.contains("failed to connect") ||
        lowerMessage.contains("connection refused") ->
            OllamaUnavailableError(cause = null)

        // Network/timeout errors
        lowerMessage.contains("timeout") ||
        lowerMessage.contains("timed out") ||
        lowerMessage.contains("deadline exceeded") ->
            ConnectionError(cause = null, host = "localhost:11434")

        // Default to unexpected error for unknown messages
        else ->
            UnexpectedError(cause = null, context = "pullModel: $errorMessage")
    }
}

class PullModelUseCase(
    private val ollamaRepository: OllamaRepository
) {
    operator fun invoke(modelName: String): Flow<PullResult> =
        ollamaRepository.pullModel(modelName).map { result ->
            when {
                result.isFailure -> {
                    val exception = result.exceptionOrNull()
                    val appError = when (exception) {
                        is OllamaApiException ->
                            exception.serverMessage?.let { mapPullError(it, modelName) }
                                ?: exception.toAppError()
                        else -> exception?.toAppError()
                            ?: UnexpectedError(cause = exception, context = "pullModel")
                    }
                    PullResult.Error(appError)
                }
                else -> {
                    val progress = result.getOrNull()
                        ?: return@map PullResult.Error(
                            UnexpectedError(cause = null, context = "pullModel: null progress")
                        )
                    progress.error?.let { errorMsg ->
                        PullResult.Error(mapPullError(errorMsg, modelName))
                    } ?: when (progress.status) {
                        "pulling manifest" -> PullResult.Starting
                        "success" -> PullResult.Completed
                        else -> PullResult.Progress(progress)
                    }
                }
            }
        }
}
