package by.ciszkin.herdmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository

class OllamaRepositoryImpl(
    private val apiService: OllamaApiService,
    private val dataStore: DataStore<Preferences>
) : OllamaRepository {

    override suspend fun getModels(): Result<List<OllamaModel>> = runCatching {
        apiService.getModels()
    }

    override suspend fun deleteModel(name: String): Result<Unit> = runCatching {
        apiService.deleteModel(name)
    }
}
