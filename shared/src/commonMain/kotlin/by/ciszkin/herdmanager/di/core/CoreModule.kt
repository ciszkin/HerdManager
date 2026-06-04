package by.ciszkin.herdmanager.di.core

import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.Module
import org.koin.dsl.module
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import by.ciszkin.herdmanager.data.api.PlatformHttpClientEngine
import org.koin.core.qualifier.named

/**
 * Core DI module providing infrastructure dependencies.
 *
 * Provides:
 * - DataStore (platform-specific via expect/actual)
 * - HTTP clients (scraper, github)
 * - ConnectionManager (lifecycle-aware connection monitoring)
 *
 * This module should be loaded during app initialization via [initKoin].
 *
 * @see by.ciszkin.herdmanager.di.initKoin
 */
val coreModule: Module = module {
    single { dataStore }

    // Scraper HTTP Client
    /**
     * HTTP client for web scraping (Ollama Registry).
     * Configured with extended timeout for slow network responses.
     */
    single<HttpClient>(named("scraper")) {
        HttpClient(PlatformHttpClientEngine) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
        }
    }

    /**
     * HTTP client for GitHub API calls (update checking).
     * Configured with JSON content negotiation and timeout.
     */
    single<HttpClient>(named("github")) {
        HttpClient(PlatformHttpClientEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
        }
    }

    /**
     * Manages connection monitoring lifecycle and API service recreation.
     *
     * IMPORTANT: Must be started explicitly via LaunchedEffect:
     * ```kotlin
     * val connectionManager: ConnectionManager = koinInject()
     * LaunchedEffect(Unit) {
     *     connectionManager.start()
     * }
     * DisposableEffect(Unit) {
     *     onDispose {
     *         connectionManager.stop()
     *     }
     * }
     * ```
     *
     * The ConnectionManager:
     * - Manages ConnectionMonitor lifecycle (start/stop)
     * - Maintains its own internal OllamaApiService for connection monitoring
     * - Observes settings changes and recreates its API service when server URL changes
     * - Provides reactive [ConnectionMonitor.state] flow for UI binding
     *
     * Note: The global OllamaApiService singleton (in [networkModule]) is created
     * once with initial settings and doesn't react to changes. ConnectionManager
     * has its own internal API service specifically for connection monitoring that
     * stays up-to-date with settings.
     *
     * @see by.ciszkin.herdmanager.di.network.networkModule
     */
    single {
        val settingsRepository: SettingsRepository = get()
        ConnectionManager(settingsRepository)
    }
}
