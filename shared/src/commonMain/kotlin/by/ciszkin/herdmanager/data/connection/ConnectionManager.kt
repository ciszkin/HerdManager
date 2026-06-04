package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.createOllamaHttpClient
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Manages connection monitoring lifecycle and API service recreation.
 *
 * Handles:
 * - ConnectionMonitor lifecycle (start/stop)
 * - Recreating API service when server URL settings change
 * - Initial API service creation from settings
 *
 * Usage:
 * ```kotlin
 * val connectionManager: ConnectionManager = koinInject()
 * LaunchedEffect(Unit) {
 *     connectionManager.start()
 * }
 *
 * DisposableEffect(Unit) {
 *     onDispose {
 *         connectionManager.stop()
 *     }
 * }
 * ```
 *
 * @param settingsRepository Repository for observing settings changes
 */
@OptIn(FlowPreview::class)
class ConnectionManager(
    private val settingsRepository: SettingsRepository
) {
    private val managerScope = CoroutineScope(Dispatchers.IO)
    private var connectionMonitor: ConnectionMonitor? = null
    private var apiService: OllamaApiService? = null
    private var currentServerUrl: String? = null
    private var isStarted = false

    companion object {
        /**
         * Debounce delay for settings changes to avoid rapid recreations
         * during UI edits (in milliseconds).
         */
        private const val SETTINGS_DEBOUNCE_MS = 500L
    }

    /**
     * Gets the current API service.
     * @throws IllegalStateException if start() hasn't been called
     */
    fun getApiService(): OllamaApiService {
        return apiService ?: throw IllegalStateException(
            "ConnectionManager not started. Call start() first."
        )
    }

    /**
     * Gets the current ConnectionMonitor for state observation.
     * Returns null if start() hasn't been called yet.
     *
     * This allows UI components to safely access the monitor during
     * the initial composition before LaunchedEffect starts the manager.
     */
    fun getConnectionMonitorOrNull(): ConnectionMonitor? = connectionMonitor

    /**
     * Starts the connection manager with initial settings.
     *
     * This function is non-suspend and returns immediately.
     * Initial settings loading and service creation happens asynchronously
     * in the background. UI components should handle the null case gracefully
     * until services are ready.
     *
     * Must be called before getApiService() returns non-null value.
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        // Launch async initialization
        managerScope.launch {
            // Get initial settings and create services
            settingsRepository.settingsFlow
                .first()
                .let { initialSettings ->
                    createServices(initialSettings.serverUrl)
                }
        }

        // Observe settings changes and recreate services when URL changes
        settingsRepository.settingsFlow
            .debounce(SETTINGS_DEBOUNCE_MS)
            .onEach { settings ->
                if (currentServerUrl != settings.serverUrl) {
                    recreateServices(settings.serverUrl)
                }
            }
            .launchIn(managerScope)
    }

    /**
     * Stops the connection manager and cleans up resources.
     */
    fun stop() {
        isStarted = false
        connectionMonitor?.stop()
        connectionMonitor = null
        apiService = null
        currentServerUrl = null
        managerScope.cancel()
    }

    private fun createServices(serverUrl: String) {
        currentServerUrl = serverUrl
        apiService = OllamaApiService(createOllamaHttpClient(serverUrl))
        connectionMonitor = ConnectionMonitor(apiService!!)
        connectionMonitor?.start(managerScope)
    }

    private fun recreateServices(serverUrl: String) {
        managerScope.launch {
            connectionMonitor?.stop()
            createServices(serverUrl)
        }
    }
}
