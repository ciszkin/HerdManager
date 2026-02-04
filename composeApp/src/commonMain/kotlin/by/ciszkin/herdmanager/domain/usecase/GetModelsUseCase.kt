package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository

class GetModelsUseCase(
    private val repository: OllamaRepository
) {
    suspend operator fun invoke(): Result<List<OllamaModel>> = repository.getModels()
}
