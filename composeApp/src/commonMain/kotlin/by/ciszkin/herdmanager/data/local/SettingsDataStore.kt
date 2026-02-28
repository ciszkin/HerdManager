package by.ciszkin.herdmanager.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import by.ciszkin.herdmanager.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun DataStore<Preferences>.settingsFlow(): Flow<Settings> = this.data.map { preferences ->
    Settings(
        serverUrl = preferences[PreferencesKeys.SERVER_URL] ?: "http://localhost:11434",
        refreshInterval = (preferences[PreferencesKeys.REFRESH_INTERVAL] ?: 5L).toInt(),
        pollingEnabled = preferences[PreferencesKeys.POLLING_ENABLED] ?: true
    )
}

suspend fun DataStore<Preferences>.saveServerUrl(url: String) {
    edit { preferences ->
        preferences[PreferencesKeys.SERVER_URL] = url
    }
}

suspend fun DataStore<Preferences>.saveRefreshInterval(interval: Int) {
    edit { preferences ->
        preferences[PreferencesKeys.REFRESH_INTERVAL] = interval.toLong().coerceIn(1, 60)
    }
}

suspend fun DataStore<Preferences>.savePollingEnabled(enabled: Boolean) {
    edit { preferences ->
        preferences[PreferencesKeys.POLLING_ENABLED] = enabled
    }
}
