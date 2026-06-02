package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Settings> = repository.settingsFlow
}
