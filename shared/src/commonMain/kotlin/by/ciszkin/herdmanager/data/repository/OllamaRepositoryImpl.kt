package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.domain.error.mapper.mapErrorWithContext
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository implementation for Ollama API operations.
 *
 * Uses [ConnectionManager] to get the current API service, ensuring that
 * server URL changes are respected without requiring app restart.
 *
 * Thread Safety: This class is thread-safe as all operations are either
 * suspend functions or return Flow, and the ConnectionManager is thread-safe.
 *
 * Lifecycle Requirements:
 * - ConnectionManager must be started before calling any repository methods
 * - This is typically handled by the platform entry point (MainActivity/main.kt)
 * via LaunchedEffect before UI composition
 *
 * @throws IllegalStateException if ConnectionManager.start() hasn't been called
 *
 * @property connectionManager Provides the current OllamaApiService
 */
class OllamaRepositoryImpl(
    private val connectionManager: ConnectionManager
) : OllamaRepository {

    /**
     * Gets the current API service from ConnectionManager.
     * This ensures we always use the latest server URL from settings.
     *
     * @throws IllegalStateException if ConnectionManager not started
     */
    private val apiService: OllamaApiService
        get() = connectionManager.getApiService()

    override suspend fun getModels(): Result<List<OllamaModel>> = runCatching {
        apiService.getModels()
    }.mapErrorWithContext(
        operation = "getModels",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override suspend fun getRunningModels(): Result<List<RunningModel>> = runCatching {
        apiService.getRunningModels()
    }.mapErrorWithContext(
        operation = "getRunningModels",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override suspend fun deleteModel(name: String): Result<Unit> = runCatching {
        apiService.deleteModel(name)
        Unit
    }.mapErrorWithContext(
        operation = "deleteModel",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
        return apiService.pullModel(modelName)
    }
}
