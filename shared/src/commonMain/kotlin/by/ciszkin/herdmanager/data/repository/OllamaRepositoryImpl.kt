package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.domain.error.mapper.mapErrorWithContext
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Repository implementation for Ollama API operations.
 *
 * Uses [ConnectionManager] to get the current API service, ensuring that
 * server URL changes are respected without requiring app restart.
 *
 * @param connectionManager Provides the current OllamaApiService
 */
class OllamaRepositoryImpl(
    private val connectionManager: ConnectionManager
) : OllamaRepository {

    override suspend fun getModels(): Result<List<OllamaModel>> = runCatching {
        connectionManager.getApiService().getModels()
    }.mapErrorWithContext(
        operation = "getModels",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override suspend fun getRunningModels(): Result<List<RunningModel>> = runCatching {
        connectionManager.getApiService().getRunningModels()
    }.mapErrorWithContext(
        operation = "getRunningModels",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override suspend fun deleteModel(name: String): Result<Unit> = runCatching {
        connectionManager.getApiService().deleteModel(name)
        Unit
    }.mapErrorWithContext(
        operation = "deleteModel",
        host = connectionManager.currentUrl ?: "unknown"
    )

    override fun pullModel(modelName: String): Flow<Result<PullProgress>> = flow {
        // Suspend until the ConnectionManager is initialized; if init fails,
        // the flow throws and the ViewModel's .catch {} handles it.
        val service = connectionManager.getApiService()
        emitAll(service.pullModel(modelName))
    }
}
