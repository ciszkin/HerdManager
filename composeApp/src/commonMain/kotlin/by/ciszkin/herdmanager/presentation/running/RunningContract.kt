package by.ciszkin.herdmanager.presentation.running

import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.presentation.architecture.MviEffect
import by.ciszkin.herdmanager.presentation.architecture.MviIntent
import by.ciszkin.herdmanager.presentation.architecture.MviState

sealed interface RunningIntent : MviIntent {
    data object Initialize : RunningIntent
    data object Refresh : RunningIntent
    data object StopPolling : RunningIntent
}

data class RunningState(
    val models: List<RunningModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pollingEnabled: Boolean = false,
    val pollingIntervalMs: Long = 0L
) : MviState

sealed interface RunningEffect : MviEffect {
    data object AnimateRefreshIcon : RunningEffect
}
