package by.ciszkin.herdmanager.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val REFRESH_INTERVAL = longPreferencesKey("refresh_interval")
    val POLLING_ENABLED = booleanPreferencesKey("polling_enabled")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
}
