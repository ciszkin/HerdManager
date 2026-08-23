package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.error.recovery.retryOnFailure
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetRegistryModelsUseCase(
    private val repository: RegistryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(query: String = "", page: Int = 1): Result<List<RegistryModel>> =
        withContext(ioDispatcher) {
            // Read-only operation: safe to auto-retry transient network/timeout errors.
            retryOnFailure {
                repository.getModels(query, page)
            }
        }
}