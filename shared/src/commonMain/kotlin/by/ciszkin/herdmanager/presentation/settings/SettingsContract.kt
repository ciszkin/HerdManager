package by.ciszkin.herdmanager.presentation.settings

import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.presentation.architecture.MviEffect
import by.ciszkin.herdmanager.presentation.architecture.MviIntent
import by.ciszkin.herdmanager.presentation.architecture.MviState

sealed interface SettingsIntent : MviIntent {
    data object LoadSettings : SettingsIntent
    data class SaveSettings(val settings: Settings) : SettingsIntent
    data class UpdateServerUrl(val url: String) : SettingsIntent
    data class UpdateRefreshInterval(val interval: Int) : SettingsIntent
    data class UpdatePollingEnabled(val enabled: Boolean) : SettingsIntent
    data class UpdateLanguage(val language: String) : SettingsIntent
    data class UpdateThemeMode(val themeMode: ThemeMode) : SettingsIntent
    data object ResetToDefaults : SettingsIntent
    data object DiscardChanges : SettingsIntent
    data object CheckForUpdates : SettingsIntent
}

data class SettingsState(
    val settings: Settings? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
    val isDirty: Boolean = false,
    val isNewVersionAvailable: Boolean = false,
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val releaseUrl: String? = null
) : MviState

sealed interface SettingsEffect : MviEffect {
    data object SettingsSaved : SettingsEffect
}
