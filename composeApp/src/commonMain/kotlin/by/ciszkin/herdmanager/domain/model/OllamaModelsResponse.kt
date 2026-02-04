package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModel>
)
