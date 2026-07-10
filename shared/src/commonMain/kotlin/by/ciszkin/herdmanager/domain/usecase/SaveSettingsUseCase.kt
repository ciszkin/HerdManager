package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveSettingsUseCase(
    private val repository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(settings: Settings): Result<Unit> = withContext(ioDispatcher) {
        repository.saveSettings(settings)
    }
}
