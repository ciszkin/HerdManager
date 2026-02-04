package by.ciszkin.herdmanager.data.api

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.OllamaModelsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

class OllamaApiService(private val client: HttpClient) {
    suspend fun getModels(): List<OllamaModel> = 
        client.get("/api/tags").body<OllamaModelsResponse>().models

    suspend fun deleteModel(name: String) = client.delete("/api/delete") {
        setBody(DeleteRequest(model = name))
    }
}

@Serializable
data class DeleteRequest(val model: String)
