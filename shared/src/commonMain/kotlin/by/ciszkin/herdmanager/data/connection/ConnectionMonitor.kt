package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ConnectionMonitor(
    private val apiService: OllamaApiService
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun start(scope: CoroutineScope) {
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                check()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun check() {
        val result = runCatching { apiService.getRunningModels() }
        _state.value = result.fold(
            onSuccess = { models ->
                if (models.isEmpty()) ConnectionState.Idle
                else ConnectionState.Running(models.size)
            },
            onFailure = { ConnectionState.Disconnected(it.message) }
        )
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 15_000L
    }
}
