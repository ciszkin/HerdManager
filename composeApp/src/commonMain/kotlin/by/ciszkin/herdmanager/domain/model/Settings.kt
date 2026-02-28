package by.ciszkin.herdmanager.domain.model

data class Settings(
    val serverUrl: String,
    val refreshInterval: Int,
    val pollingEnabled: Boolean
)
