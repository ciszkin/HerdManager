package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.scraping.OllamaLibraryScraper
import by.ciszkin.herdmanager.domain.error.mapper.mapErrorWithContext
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import by.ciszkin.herdmanager.domain.repository.RegistryRepository

class RegistryRepositoryImpl : RegistryRepository {
    override suspend fun getModels(
        query: String,
        page: Int,
        sort: RegistrySort,
        category: String?
    ): Result<List<RegistryModel>> =
        OllamaLibraryScraper.fetchModels(query, page, sort, category)
            .mapErrorWithContext(
                operation = "getRegistryModels"
            )
}
