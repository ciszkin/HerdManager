package by.ciszkin.herdmanager.presentation.running

import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.presentation.architecture.MviEffect
import by.ciszkin.herdmanager.presentation.architecture.MviIntent
import by.ciszkin.herdmanager.presentation.architecture.MviState

sealed interface RunningIntent : MviIntent {
    data object LoadModels : RunningIntent
    data object Retry : RunningIntent
    data object StartPolling : RunningIntent
    data object StopPolling : RunningIntent
}

data class RunningState(
    val models: List<RunningModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPolling: Boolean = false
) : MviState

sealed interface RunningEffect : MviEffect {
    data object AnimateRefreshIcon : RunningEffect
}
