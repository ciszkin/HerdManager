package by.ciszkin.herdmanager.presentation.architecture

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseMviViewModel<I : MviIntent, S : MviState, E : MviEffect> : ScreenModel {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>()
    val effect: Flow<E> = _effect.receiveAsFlow()

    abstract fun initialState(): S
    abstract fun onIntent(intent: I)

    protected fun reduceState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    protected fun sendEffect(effect: E) {
        screenModelScope.launch { _effect.send(effect) }
    }
}
