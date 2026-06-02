package by.ciszkin.herdmanager.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaModel(
    val name: String,
    val model: String,
    @SerialName("modified_at")
    val modifiedAt: String,
    val size: Long,
    val digest: String,
    val details: ModelDetails? = null
)

@Serializable
data class ModelDetails(
    @SerialName("parent_model")
    val parentModel: String,
    val format: String,
    val family: String,
    val families: List<String>,
    @SerialName("parameter_size")
    val parameterSize: String,
    @SerialName("quantization_level")
    val quantizationLevel: String
)
