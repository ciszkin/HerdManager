package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteModelUseCase(
    private val repository: OllamaRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(modelName: String): Result<Unit> = withContext(ioDispatcher) {
        repository.deleteModel(modelName)
    }
}
