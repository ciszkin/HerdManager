package by.ciszkin.herdmanager.domain.model

sealed interface ConnectionState {
    data object Unknown : ConnectionState
    data object Idle : ConnectionState
    data class Running(val count: Int) : ConnectionState
    data class Disconnected(val reason: String? = null) : ConnectionState
}
