package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.createOllamaHttpClient
import by.ciszkin.herdmanager.data.api.PlatformHttpClientEngine
import by.ciszkin.herdmanager.data.repository.OllamaRepositoryImpl
import by.ciszkin.herdmanager.data.repository.RegistryRepositoryImpl
import by.ciszkin.herdmanager.data.repository.SettingsRepositoryImpl
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import by.ciszkin.herdmanager.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object AppModule {
    private val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(dataStore) }

    private val apiService by lazy {
        val settings = runBlocking { settingsRepository.settingsFlow.first() }
        OllamaApiService(createOllamaHttpClient(settings.serverUrl))
    }
    private val repository: OllamaRepository by lazy { OllamaRepositoryImpl(apiService) }
    private val registryRepository: RegistryRepository by lazy { RegistryRepositoryImpl() }

    val scraperHttpClient by lazy {
        HttpClient(PlatformHttpClientEngine) {
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
}
