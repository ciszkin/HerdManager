package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class PullModelUseCase(
    private val ollamaRepository: OllamaRepository
) {
    operator fun invoke(modelName: String): Flow<PullResult> =
        ollamaRepository.pullModel(modelName).mapNotNull { result ->
            result.getOrNull()?.let { progress ->
                progress.error?.let {
                    PullResult.Error(it)
                } ?: when (progress.status) {
                    "pulling manifest" -> PullResult.Starting
                    "success" -> PullResult.Completed
                    else -> PullResult.Progress(progress)
                }
            }
        }
}
