package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetRunningModelsUseCase(
    private val repository: OllamaRepository
) {
    suspend operator fun invoke(): Result<List<RunningModel>> = withContext(Dispatchers.IO) {
        repository.getRunningModels()
    }
}
