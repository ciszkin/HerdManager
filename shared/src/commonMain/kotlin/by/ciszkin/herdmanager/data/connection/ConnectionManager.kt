package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.createOllamaHttpClient
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
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
 * @param clientFactory Creates the HTTP client for an Ollama server URL.
 * Defaults to [createOllamaHttpClient]; injectable for tests.
 */
@OptIn(FlowPreview::class)
class ConnectionManager(
    private val settingsRepository: SettingsRepository,
    private val clientFactory: (serverUrl: String) -> HttpClient = ::createOllamaHttpClient
) {
    @Volatile
    private var managerScope: CoroutineScope? = null

    @Volatile
    private var connectionMonitor: ConnectionMonitor? = null

    @Volatile
    private var apiService: OllamaApiService? = null

    @Volatile
    private var currentServerUrl: String? = null

    @Volatile
    private var isStarted = false

    @Volatile
    private var currentClient: HttpClient? = null

    @Volatile
    private var initDeferred: CompletableDeferred<Unit>? = null

    companion object {
        private const val SETTINGS_DEBOUNCE_MS = 500L
    }

    /**
     * Gets the current API service, suspending until initialization completes.
     * @throws IllegalStateException if start() hasn't been called
     */
    suspend fun getApiService(): OllamaApiService {
        initDeferred?.await()
        return apiService ?: throw IllegalStateException(
            "ConnectionManager not initialized."
        )
    }

    /**
     * Gets the current server URL.
     * Returns null if start() hasn't been called yet.
     */
    val currentUrl: String?
        get() = currentServerUrl

    /**
     * Gets the current ConnectionMonitor for state observation.
     * Returns null if start() hasn't been called yet.
     */
    fun getConnectionMonitorOrNull(): ConnectionMonitor? = connectionMonitor

    /**
     * Starts the connection manager with initial settings.
     * Safe to call after stop() — recreates the coroutine scope and services.
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        managerScope = scope
        val deferred = CompletableDeferred<Unit>()
        initDeferred = deferred

        // Async initialization from current settings
        scope.launch {
            try {
                val initialSettings = settingsRepository.settingsFlow.first()
                // Guard against stop()/start() while init is in flight: a cancelled
                // scope must not create services over the fresh one.
                if (scope.isActive) {
                    createServices(scope, initialSettings.serverUrl)
                }
                deferred.complete(Unit)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
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
            .launchIn(scope)
    }

    /**
     * Stops the connection manager and cleans up resources.
     * start() can be called again after stop().
     */
    fun stop() {
        isStarted = false
        connectionMonitor?.stop()
        connectionMonitor = null
        currentClient?.close()
        currentClient = null
        apiService = null
        currentServerUrl = null
        // Unblock waiters first so they see a predictable "not initialized"
        // IllegalStateException; the scope.isActive guard in the init coroutine
        // prevents stale service creation on the cancelled scope.
        initDeferred?.complete(Unit)
        initDeferred = null
        managerScope?.cancel()
        managerScope = null
    }

    private fun createServices(scope: CoroutineScope, serverUrl: String) {
        currentServerUrl = serverUrl
        currentClient?.close()
        val client = clientFactory(serverUrl)
        currentClient = client
        apiService = OllamaApiService(client)
        connectionMonitor = ConnectionMonitor(apiService!!)
        connectionMonitor?.start(scope)
    }

    private fun recreateServices(serverUrl: String) {
        val scope = managerScope ?: return
        val monitor = connectionMonitor
        scope.launch {
            monitor?.stop()
            createServices(scope, serverUrl)
        }
    }
}
