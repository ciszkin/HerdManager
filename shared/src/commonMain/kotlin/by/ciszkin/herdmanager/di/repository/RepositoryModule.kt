package by.ciszkin.herdmanager.di.repository

import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.data.repository.OllamaRepositoryImpl
import by.ciszkin.herdmanager.data.repository.RegistryRepositoryImpl
import by.ciszkin.herdmanager.data.repository.SettingsRepositoryImpl
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Repository DI module providing data layer implementations.
 *
 * Provides:
 * - [SettingsRepository] - App settings persistence via DataStore
 * - [OllamaRepository] - Ollama API operations (models, running, pull)
 * - [RegistryRepository] - Ollama Registry model browsing
 *
 * This module bridges the domain layer (use cases) with the data layer.
 * All repositories are singletons for efficiency and state sharing.
 *
 * This module should be loaded during app initialization via [initKoin].
 *
 * @see by.ciszkin.herdmanager.di.initKoin
 */
val repositoryModule: Module = module {
    /**
     * Settings repository using DataStore for persistence.
     * Provides reactive settings observation via [SettingsRepository.settingsFlow].
     */
    single<SettingsRepository> { SettingsRepositoryImpl(dataStore) }

    /**
     * Ollama repository for model management operations.
     *
     * IMPORTANT: Uses [ConnectionManager] to get the current API service.
     * This ensures that when the server URL setting changes, the repository
     * will use the updated URL without requiring app restart.
     *
     * ConnectionManager must be started before repository methods are called.
     * This is typically done via LaunchedEffect in MainActivity or main().
     *
     * @see by.ciszkin.herdmanager.data.connection.ConnectionManager.getApiService
     */
    single<OllamaRepository> { OllamaRepositoryImpl(get<ConnectionManager>()) }

    /**
     * Ollama Registry repository for browsing available models.
     * Uses web scraping to fetch model information from ollama.com/library.
     */
    single<RegistryRepository> { RegistryRepositoryImpl() }
}
