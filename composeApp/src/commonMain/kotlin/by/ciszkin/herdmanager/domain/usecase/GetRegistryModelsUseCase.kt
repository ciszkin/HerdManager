package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetRegistryModelsUseCase(
    private val repository: RegistryRepository
) {
    suspend operator fun invoke(query: String = "", page: Int = 1): Result<List<RegistryModel>> = withContext(Dispatchers.IO) {
        repository.getModels(query, page)
    }
}
