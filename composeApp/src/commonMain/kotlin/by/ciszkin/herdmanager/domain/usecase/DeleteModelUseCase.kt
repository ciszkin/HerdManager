package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteModelUseCase(
    private val repository: OllamaRepository
) {
    suspend operator fun invoke(modelName: String): Result<Unit> = withContext(Dispatchers.IO) {
        repository.deleteModel(modelName)
    }
}
