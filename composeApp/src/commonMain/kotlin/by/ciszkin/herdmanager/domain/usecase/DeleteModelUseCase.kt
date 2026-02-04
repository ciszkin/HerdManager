package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.repository.OllamaRepository

class DeleteModelUseCase(
    private val repository: OllamaRepository
) {
    suspend operator fun invoke(modelName: String): Result<Unit> = repository.deleteModel(modelName)
}
