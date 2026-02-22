package by.ciszkin.herdmanager.data.api

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.OllamaModelsResponse
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.model.RunningModelsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.INFINITE

class OllamaApiService(private val client: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getModels(): List<OllamaModel> =
        client.get("/api/tags").body<OllamaModelsResponse>().models

    suspend fun getRunningModels(): List<RunningModel> =
        client.get("/api/ps").body<RunningModelsResponse>().models

    suspend fun deleteModel(name: String) = client.delete("/api/delete") {
        setBody(DeleteRequest(model = name))
    }

    fun pullModel(
        modelName: String
    ): Flow<Result<PullProgress>> = callbackFlow {
        val call = client.preparePost("/api/pull") {
            timeout {
                requestTimeoutMillis = INFINITE.inWholeMilliseconds
                connectTimeoutMillis = 30_000
            }
            contentType(ContentType.Application.Json)
            setBody(
                PullRequest(
                    model = modelName,
                    stream = true
                )
            )
        }

        call.execute { response ->
            val channel = response.bodyAsChannel()
            var buffer = ByteArray(4096)
            var position = 0
            var partialLine = ""

            while (true) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size - position)

                if (bytesRead < 0) {
                    if (partialLine.isNotEmpty()) {
                        try {
                            val progress = json.decodeFromString<PullProgress>(partialLine)
                            trySend(Result.success(progress))
                        } catch (e: Exception) {
                            trySend(Result.failure(e))
                        }
                    }
                    break
                }

                position += bytesRead
                val fullData = buffer.decodeToString(0, position)

                val lines = fullData.split('\n')
                partialLine = lines.last()

                lines.dropLast(1).forEach { line ->
                    val trimmed = line.trimEnd('\r')
                    if (trimmed.isNotEmpty()) {
                        try {
                            val progress = json.decodeFromString<PullProgress>(trimmed)
                            trySend(Result.success(progress))
                        } catch (e: Exception) {
                            trySend(Result.failure(e))
                        }
                    }
                }

                val remaining = partialLine.encodeToByteArray()
                buffer = ByteArray(4096)
                remaining.copyInto(buffer)
                position = remaining.size
            }
        }

        awaitClose {
            cancel()
        }
    }.flowOn(Dispatchers.IO)
}

@Serializable
data class DeleteRequest(val model: String)

@Serializable
data class PullRequest(
    val model: String,
    val stream: Boolean? = null
)
