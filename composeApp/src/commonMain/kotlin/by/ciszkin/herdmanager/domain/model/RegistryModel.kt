package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistryModel(
    val id: String,
    val name: String,
    val description: String,
    val pullCount: Long,
    val tags: List<String>,
    val capabilities: List<String>,
    val updatedAt: String?
)
