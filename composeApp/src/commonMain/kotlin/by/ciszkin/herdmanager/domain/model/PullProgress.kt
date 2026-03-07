package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PullProgress(
    val status: String? = null,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null,
    val error: String? = null
)
