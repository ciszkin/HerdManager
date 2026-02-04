package by.ciszkin.herdmanager.data.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object OllamaApiClient {
    private const val DEFAULT_BASE_URL = "http://192.168.100.12:11434"

    val client = HttpClient(PlatformHttpClientEngine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = Logger.SIMPLE
        }
        defaultRequest {
            url(DEFAULT_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
