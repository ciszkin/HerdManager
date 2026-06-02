package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaVersion(
    @SerialName("version")
    val version: String
)
