package by.ciszkin.herdmanager.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun DataStore<Preferences>.settingsFlow(): Flow<Settings> = this.data.map { preferences ->
    Settings(
        serverUrl = preferences[PreferencesKeys.SERVER_URL] ?: "http://localhost:11434",
        refreshInterval = (preferences[PreferencesKeys.REFRESH_INTERVAL] ?: 5L).toInt(),
        pollingEnabled = preferences[PreferencesKeys.POLLING_ENABLED] ?: true,
        language = preferences[PreferencesKeys.LANGUAGE] ?: "en",
        themeMode = ThemeMode.fromValue(preferences[PreferencesKeys.THEME_MODE] ?: "system")
    )
}

/**
 * Persists the whole [Settings] in a single transactional edit so a partial
 * failure cannot leave the preferences in a mixed state.
 */
suspend fun DataStore<Preferences>.saveSettings(settings: Settings) {
    edit { preferences ->
        preferences[PreferencesKeys.SERVER_URL] = settings.serverUrl
        preferences[PreferencesKeys.REFRESH_INTERVAL] = settings.refreshInterval.toLong().coerceIn(1, 60)
        preferences[PreferencesKeys.POLLING_ENABLED] = settings.pollingEnabled
        preferences[PreferencesKeys.LANGUAGE] = settings.language
        preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.value
    }
}

suspend fun DataStore<Preferences>.saveCurrentVersion(version: String) {
    edit { preferences ->
        preferences[PreferencesKeys.OLLAMA_CURRENT_VERSION] = version
    }
}

suspend fun DataStore<Preferences>.saveLatestVersion(version: String) {
    edit { preferences ->
        preferences[PreferencesKeys.OLLAMA_LATEST_VERSION] = version
    }
}

fun DataStore<Preferences>.currentVersionFlow(): Flow<String?> = this.data.map { preferences ->
    preferences[PreferencesKeys.OLLAMA_CURRENT_VERSION]
}

fun DataStore<Preferences>.latestVersionFlow(): Flow<String?> = this.data.map { preferences ->
    preferences[PreferencesKeys.OLLAMA_LATEST_VERSION]
}

fun DataStore<Preferences>.lastUpdateCheckFlow(): Flow<Long> = this.data.map { preferences ->
    preferences[PreferencesKeys.LAST_UPDATE_CHECK] ?: 0L
}

suspend fun DataStore<Preferences>.saveLastUpdateCheck(timestampMillis: Long) {
    edit { preferences ->
        preferences[PreferencesKeys.LAST_UPDATE_CHECK] = timestampMillis
    }
}
