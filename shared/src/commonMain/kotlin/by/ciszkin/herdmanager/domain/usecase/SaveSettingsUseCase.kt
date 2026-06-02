package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings): Result<Unit> = withContext(Dispatchers.IO) {
        repository.saveSettings(settings)
    }
}
