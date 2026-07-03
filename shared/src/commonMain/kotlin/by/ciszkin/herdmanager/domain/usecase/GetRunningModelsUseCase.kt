package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.error.recovery.retryOnFailure
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository

class GetRunningModelsUseCase(
    private val repository: OllamaRepository
) {
    suspend operator fun invoke(): Result<List<RunningModel>> {
        return retryOnFailure {
            repository.getRunningModels()
        }
    }
}
