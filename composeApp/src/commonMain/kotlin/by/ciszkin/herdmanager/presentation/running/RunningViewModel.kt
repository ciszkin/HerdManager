package by.ciszkin.herdmanager.presentation.running

import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class RunningViewModel(
    private val getRunningModelsUseCase: GetRunningModelsUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase
) : BaseMviViewModel<RunningIntent, RunningState, RunningEffect>() {

    private var pollingJob: Job? = null

    override fun initialState() = RunningState()

    override fun onIntent(intent: RunningIntent) {
        when (intent) {
            RunningIntent.Initialize -> initialize()
            RunningIntent.Refresh -> refresh()
            RunningIntent.StopPolling -> stopPolling()
        }
    }

    private fun initialize() {
        screenModelScope.launch {
            observeSettingsUseCase().collectLatest { settings ->
                val pollingIntervalMs = settings.refreshInterval * 1000L
                val pollingEnabled = settings.pollingEnabled

                reduceState {
                    copy(
                        pollingEnabled = pollingEnabled,
                        pollingIntervalMs = pollingIntervalMs
                    )
                }

                if (pollingEnabled) {
                    if (isActive) {
                        startPolling()
                    } else {
                        stopPolling()
                    }
                } else {
                    stopPolling()
                }
            }
        }
    }

    private fun loadModels() {
        screenModelScope.launch {
            reduceState { copy(isLoading = true, error = null) }
            delay(100)
            getRunningModelsUseCase()
                .onSuccess { models ->
                    reduceState { copy(models = models, isLoading = false) }
                }
                .onFailure { error ->
                    reduceState { copy(error = error.message, isLoading = false) }
                }
        }
    }

    private fun refresh() {
        sendEffect(RunningEffect.AnimateRefreshIcon)
        loadModels()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = screenModelScope.launch {
            do {
                refresh()
                delay(state.value.pollingIntervalMs)
            } while (isActive)
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        reduceState { copy(pollingEnabled = false) }
    }
}
