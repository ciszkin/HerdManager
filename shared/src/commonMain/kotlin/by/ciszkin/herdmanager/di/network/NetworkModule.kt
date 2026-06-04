package by.ciszkin.herdmanager.di.network

import by.ciszkin.herdmanager.data.api.GitHubApiService
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.createOllamaHttpClient
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.qualifier.named

/**
 * Network DI module providing API service dependencies.
 *
 * Provides:
 * - OllamaApiService (singleton, created with initial settings)
 * - GitHubApiService (singleton, update checking)
 *
 * NOTE: Using [runBlocking] to fetch initial settings synchronously during DI
 * initialization. This is acceptable because:
 * 1. It runs once during app startup (not in UI code path)
 * 2. DataStore settings are cached (fast retrieval)
 * 3. It's called before UI rendering (off main thread on Android, in main() on Desktop)
 *
 * For dynamic API service that reacts to settings changes, see [ConnectionManager]
 * which manages its own internal API service for connection monitoring.
 *
 * This module should be loaded during app initialization via [initKoin].
 *
 * @see by.ciszkin.herdmanager.di.initKoin
 * @see by.ciszkin.herdmanager.data.connection.ConnectionManager
 */
val networkModule: Module = module {
    /**
     * Ollama API service singleton created with initial settings.
     *
     * IMPORTANT: This service is created once during app initialization with
     * the server URL from settings. If the server URL changes during runtime,
     * this service will continue using the original URL.
     *
     * For connection monitoring that reacts to settings changes, ConnectionManager
     * maintains its own internal API service that gets recreated when URL changes.
     *
     * Most use cases (delete model, pull model, get version) work fine with this
     * static service since they're invoked in response to user actions (user can
     * simply update settings and retry if needed).
     */
    single {
        val settingsRepository: SettingsRepository = get()
        val settings = runBlocking {
            settingsRepository.settingsFlow.first()
        }
        OllamaApiService(createOllamaHttpClient(settings.serverUrl))
    }

    /**
     * GitHub API service for update checking.
     * Uses the configured GitHub HTTP client.
     */
    single {
        val githubClient: HttpClient = get(named("github"))
        GitHubApiService(githubClient)
    }
}
