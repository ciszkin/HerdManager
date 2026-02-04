package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.data.api.OllamaApiClient
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.repository.OllamaRepositoryImpl
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase

object AppModule {
    private val apiService by lazy { OllamaApiService(OllamaApiClient.client) }
    private val repository: OllamaRepository by lazy { OllamaRepositoryImpl(apiService, dataStore) }

    val getModelsUseCase by lazy { GetModelsUseCase(repository) }
    val deleteModelUseCase by lazy { DeleteModelUseCase(repository) }
}
