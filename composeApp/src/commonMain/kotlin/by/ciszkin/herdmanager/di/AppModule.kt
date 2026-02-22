package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.data.api.OllamaApiClient
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.api.PlatformHttpClientEngine
import by.ciszkin.herdmanager.data.repository.OllamaRepositoryImpl
import by.ciszkin.herdmanager.data.repository.RegistryRepositoryImpl
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

object AppModule {
    private val apiService by lazy { OllamaApiService(OllamaApiClient.client) }
    private val repository: OllamaRepository by lazy { OllamaRepositoryImpl(apiService, dataStore) }
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
}
