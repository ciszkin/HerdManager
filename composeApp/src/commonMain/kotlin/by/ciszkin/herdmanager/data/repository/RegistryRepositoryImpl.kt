package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.scraping.OllamaLibraryScraper
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.repository.RegistryRepository

class RegistryRepositoryImpl : RegistryRepository {

    override suspend fun getModels(query: String, page: Int): Result<List<RegistryModel>> =
        OllamaLibraryScraper.fetchModels(query, page)
}
