package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import kotlinx.coroutines.flow.Flow

interface OllamaRepository {
    suspend fun getModels(): Result<List<OllamaModel>>
    suspend fun deleteModel(name: String): Result<Unit>
    fun pullModel(modelName: String): Flow<Result<PullProgress>>
}
