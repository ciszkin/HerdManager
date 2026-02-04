package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.OllamaModel

interface OllamaRepository {
    suspend fun getModels(): Result<List<OllamaModel>>
    suspend fun deleteModel(name: String): Result<Unit>
}
