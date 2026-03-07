package by.ciszkin.herdmanager.presentation.settings

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.domain.usecase.SaveSettingsUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : BaseMviViewModel<SettingsIntent, SettingsState, SettingsEffect>() {

    private var originalSettings: Settings? = null
    private var currentSettings: Settings? = null

    override fun initialState() = SettingsState()

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.LoadSettings -> loadSettings()
            is SettingsIntent.SaveSettings -> saveSettings(intent.settings)
            is SettingsIntent.UpdateServerUrl -> updateServerUrl(intent.url)
            is SettingsIntent.UpdateRefreshInterval -> updateRefreshInterval(intent.interval)
            is SettingsIntent.UpdatePollingEnabled -> updatePollingEnabled(intent.enabled)
            is SettingsIntent.UpdateLanguage -> updateLanguage(intent.language)
            is SettingsIntent.UpdateThemeMode -> updateThemeMode(intent.themeMode)
            SettingsIntent.ResetToDefaults -> resetToDefaults()
            SettingsIntent.DiscardChanges -> discardChanges()
        }
    }

    private fun loadSettings() {
        screenModelScope.launch {
            delay(100)
            observeSettingsUseCase().first().also { settings ->
                originalSettings = settings
                currentSettings = settings
                reduceState { copy(settings = settings, isDirty = false) }
            }
        }
    }

    private fun updateServerUrl(url: String) {
        currentSettings?.let { current ->
            val updated = current.copy(serverUrl = url)
            currentSettings = updated
            reduceState { copy(settings = currentSettings, isDirty = updated != originalSettings) }
        }
    }

    private fun updateRefreshInterval(interval: Int) {
        currentSettings?.let { current ->
            val updated = current.copy(refreshInterval = interval)
            currentSettings = updated
            reduceState { copy(settings = currentSettings, isDirty = updated != originalSettings) }
        }
    }

    private fun updatePollingEnabled(enabled: Boolean) {
        currentSettings?.let { current ->
            val updated = current.copy(pollingEnabled = enabled)
            currentSettings = updated
            reduceState { copy(settings = currentSettings, isDirty = updated != originalSettings) }
        }
    }

    private fun updateLanguage(language: String) {
        currentSettings?.let { current ->
            val updated = current.copy(language = language)
            currentSettings = updated
            reduceState { copy(settings = currentSettings, isDirty = updated != originalSettings) }
        }
    }

    private fun updateThemeMode(themeMode: ThemeMode) {
        currentSettings?.let { current ->
            val updated = current.copy(themeMode = themeMode)
            currentSettings = updated
            reduceState { copy(settings = currentSettings, isDirty = updated != originalSettings) }
        }
    }

    private fun saveSettings(settings: Settings) {
        val previousLanguage = originalSettings?.language
        screenModelScope.launch {
            reduceState { copy(isSaving = true, saveError = null) }
            saveSettingsUseCase(settings)
                .onSuccess {
                    currentSettings = settings
                    originalSettings = settings
                    reduceState { copy(isSaving = false, isDirty = false) }
                    sendEffect(SettingsEffect.SettingsSaved)
                    if (previousLanguage != settings.language) {
                        setLocale(settings.language)
                    }
                }
                .onFailure { error ->
                    reduceState {
                        copy(
                            isSaving = false,
                            saveError = error.message
                        )
                    }
                }
        }
    }

    private fun resetToDefaults() {
        val defaults = Settings(
            serverUrl = "http://localhost:11434",
            refreshInterval = 5,
            pollingEnabled = true,
            language = "en",
            themeMode = ThemeMode.SYSTEM
        )
        currentSettings = defaults
        reduceState { copy(settings = defaults, isDirty = defaults != originalSettings) }
    }

    private fun discardChanges() {
        originalSettings?.let { original ->
            currentSettings = original
            reduceState { copy(settings = original, isDirty = false) }
        }
    }
}
