package by.ciszkin.herdmanager.presentation.running

import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class RunningViewModel(
    private val getRunningModelsUseCase: GetRunningModelsUseCase
) : BaseMviViewModel<RunningIntent, RunningState, RunningEffect>() {

    private var pollingJob: Job? = null
    private val pollingInterval = 5000L

    override fun initialState() = RunningState()

    override fun onIntent(intent: RunningIntent) {
        when (intent) {
            RunningIntent.LoadModels -> loadModels()
            RunningIntent.Retry -> refresh()
            RunningIntent.StartPolling -> startPolling()
            RunningIntent.StopPolling -> stopPolling()
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
            reduceState { copy(isPolling = true) }
            do {
                refresh()
                delay(pollingInterval)
            } while (isActive)
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        reduceState { copy(isPolling = false) }
    }
}
