package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors connection to Ollama server by polling for running models.
 *
 * Emits connection state changes through [state] StateFlow:
 * - Initially emits [ConnectionState.Unknown] immediately
 * - Updates state every [CHECK_INTERVAL_MS] milliseconds
 * - Emits [ConnectionState.Running] with count when models are running
 * - Emits [ConnectionState.Idle] when server is reachable but no models running
 * - Emits [ConnectionState.Disconnected] when server is unreachable
 *
 * @param apiService API service for checking connection status
 */
class ConnectionMonitor(
    private val apiService: OllamaApiService
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)

    /**
     * StateFlow emitting connection state changes.
     * Immediately emits [ConnectionState.Unknown] to new subscribers.
     */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Volatile
    private var pollingJob: Job? = null

    /**
     * Starts monitoring connection with periodic polling.
     *
     * The first check happens immediately, then every [CHECK_INTERVAL_MS].
     * Runs on the given scope's dispatcher; the caller controls lifecycle
     * (e.g. a scope backed by Dispatchers.IO in production, a test scope
     * with virtual time in tests).
     */
    fun start(scope: CoroutineScope) {
        pollingJob = scope.launch {
            while (isActive) {
                check()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Stops monitoring connection and cleans up resources.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
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
        /**
         * Interval between connection checks in milliseconds.
         */
        private const val CHECK_INTERVAL_MS = 15_000L
    }
}
