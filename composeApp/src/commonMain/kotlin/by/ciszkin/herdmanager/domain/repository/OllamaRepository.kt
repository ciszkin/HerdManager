package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import kotlinx.coroutines.flow.Flow

interface OllamaRepository {
    suspend fun getModels(): Result<List<OllamaModel>>
    suspend fun getRunningModels(): Result<List<RunningModel>>
    suspend fun deleteModel(name: String): Result<Unit>
    fun pullModel(modelName: String): Flow<Result<PullProgress>>
}
