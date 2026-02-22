package by.ciszkin.herdmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.flow.Flow

class OllamaRepositoryImpl(
    private val apiService: OllamaApiService,
    private val dataStore: DataStore<Preferences>
) : OllamaRepository {

    override suspend fun getModels(): Result<List<OllamaModel>> = runCatching {
        apiService.getModels()
    }

    override suspend fun getRunningModels(): Result<List<RunningModel>> = runCatching {
        apiService.getRunningModels()
    }

    override suspend fun deleteModel(name: String): Result<Unit> = runCatching {
        apiService.deleteModel(name)
    }

    override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
        return apiService.pullModel(modelName)
    }
}
