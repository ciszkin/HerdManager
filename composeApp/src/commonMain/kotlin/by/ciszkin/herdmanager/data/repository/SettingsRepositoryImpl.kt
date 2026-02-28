package by.ciszkin.herdmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import by.ciszkin.herdmanager.data.local.savePollingEnabled
import by.ciszkin.herdmanager.data.local.saveRefreshInterval
import by.ciszkin.herdmanager.data.local.saveServerUrl
import by.ciszkin.herdmanager.data.local.settingsFlow
import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val _settingsFlow = MutableStateFlow<Settings?>(null)
    override val settingsFlow: Flow<Settings> = _settingsFlow.filterNotNull()

    init {
        dataStore.settingsFlow()
            .onEach { settings -> _settingsFlow.value = settings }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> = kotlin.runCatching {
        dataStore.saveServerUrl(settings.serverUrl)
        dataStore.saveRefreshInterval(settings.refreshInterval)
        dataStore.savePollingEnabled(settings.pollingEnabled)
    }

    suspend fun getSettings(): Settings = settingsFlow.first()
}
