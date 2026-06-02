package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RunningModelsResponse(
    val models: List<RunningModel>
)
