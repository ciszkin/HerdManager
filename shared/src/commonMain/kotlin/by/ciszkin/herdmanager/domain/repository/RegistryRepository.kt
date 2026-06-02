package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.RegistryModel
import kotlin.Result

interface RegistryRepository {
    suspend fun getModels(query: String = "", page: Int = 1): Result<List<RegistryModel>>
}

