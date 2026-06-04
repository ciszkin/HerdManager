package by.ciszkin.herdmanager.di.usecase

import by.ciszkin.herdmanager.domain.usecase.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Use Case DI module providing domain layer business logic.
 *
 * Provides:
 * - Model management: [GetModelsUseCase], [DeleteModelUseCase], [GetRunningModelsUseCase], [PullModelUseCase]
 * - Registry: [GetRegistryModelsUseCase]
 * - Settings: [ObserveSettingsUseCase], [SaveSettingsUseCase]
 * - Update checking: [CheckForOllamaUpdateUseCase]
 *
 * Use cases are the entry point for the domain layer and are injected
 * by ViewModels following the MVI pattern.
 *
 * All use cases are singletons for efficiency and dependency sharing.
 * This module should be loaded during app initialization via [initKoin].
 *
 * @see by.ciszkin.herdmanager.di.initKoin
 * @see by.ciszkin.herdmanager.presentation.BaseMviViewModel
 */
val useCaseModule: Module = module {
    /**
     * Fetches available models from Ollama API.
     */
    single { GetModelsUseCase(get()) }

    /**
     * Deletes a model from Ollama.
     */
    single { DeleteModelUseCase(get()) }

    /**
     * Gets currently running models.
     */
    single { GetRunningModelsUseCase(get()) }

    /**
     * Pulls/downloads a model from Ollama registry.
     */
    single { PullModelUseCase(get()) }

    /**
     * Fetches models from Ollama Registry (ollama.com/library).
     */
    single { GetRegistryModelsUseCase(get()) }

    /**
     * Observes app settings reactively via Flow.
     */
    single { ObserveSettingsUseCase(get()) }

    /**
     * Saves app settings to DataStore.
     */
    single { SaveSettingsUseCase(get()) }

    /**
     * Checks for Ollama updates via GitHub API.
     * Requires both [OllamaRepository] and [GitHubApiService].
     */
    single { CheckForOllamaUpdateUseCase(get(), get()) }
}
