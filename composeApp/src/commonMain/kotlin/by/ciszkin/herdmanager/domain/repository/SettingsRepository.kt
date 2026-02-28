package by.ciszkin.herdmanager.domain.repository

import by.ciszkin.herdmanager.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface SettingsRepository {
    suspend fun saveSettings(settings: Settings): Result<Unit>
    val settingsFlow: Flow<Settings>
}
