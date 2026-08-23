package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import kotlin.Result

interface RegistryRepository {
    suspend fun getModels(
        query: String = "",
        page: Int = 1,
        sort: RegistrySort = RegistrySort.POPULAR,
        category: String? = null
    ): Result<List<RegistryModel>>
}

