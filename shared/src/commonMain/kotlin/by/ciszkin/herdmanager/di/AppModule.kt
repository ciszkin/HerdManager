package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.data.api.GitHubApiService
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.PlatformHttpClientEngine
import by.ciszkin.herdmanager.data.api.createOllamaHttpClient
import by.ciszkin.herdmanager.data.connection.ConnectionMonitor
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.data.repository.OllamaRepositoryImpl
import by.ciszkin.herdmanager.data.repository.SettingsRepositoryImpl
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import by.ciszkin.herdmanager.domain.usecase.CheckForOllamaUpdateUseCase
import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import by.ciszkin.herdmanager.domain.usecase.SaveSettingsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

expect fun getRegistryRepository(): RegistryRepository

object AppModule {
    private val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(dataStore) }

    val connectionMonitor by lazy {
        ConnectionMonitor(apiService).also {
            it.start(CoroutineScope(Dispatchers.IO))
        }
    }

    private val apiService by lazy {
        val settings = runBlocking { settingsRepository.settingsFlow.first() }
        OllamaApiService(createOllamaHttpClient(settings.serverUrl))
    }
    private val repository: OllamaRepository by lazy {
        OllamaRepositoryImpl(apiService) }

    private val registryRepository: RegistryRepository by lazy { getRegistryRepository() }

    val scraperHttpClient by lazy {
        HttpClient(PlatformHttpClientEngine) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
        }
    }

    private val githubHttpClient by lazy {
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

    val getModelsUseCase by lazy { GetModelsUseCase(repository) }
    val deleteModelUseCase by lazy { DeleteModelUseCase(repository) }
    val getRegistryModelsUseCase by lazy { GetRegistryModelsUseCase(registryRepository) }
    val getRunningModelsUseCase by lazy { GetRunningModelsUseCase(repository) }
    val pullModelUseCase by lazy { PullModelUseCase(repository) }
    val observeSettingsUseCase by lazy { ObserveSettingsUseCase(settingsRepository) }
    val saveSettingsUseCase by lazy { SaveSettingsUseCase(settingsRepository) }
    private val githubApiService by lazy { GitHubApiService(githubHttpClient) }
    val checkForOllamaUpdateUseCase by lazy {
        CheckForOllamaUpdateUseCase(apiService, githubApiService)
    }
}
