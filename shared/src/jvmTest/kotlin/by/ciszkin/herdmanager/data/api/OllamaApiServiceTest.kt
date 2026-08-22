package by.ciszkin.herdmanager.data.api

import by.ciszkin.herdmanager.domain.error.OllamaApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OllamaApiServiceTest {

    private fun clientWith(handler: io.ktor.client.engine.mock.MockEngineConfig.() -> Unit): HttpClient = HttpClient(MockEngine) {
        engine(handler)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        defaultRequest {
            url("http://localhost:11434")
            contentType(ContentType.Application.Json)
        }
    }

    @Test
    fun `getModels returns parsed models on success`() = runTest {
        val client = clientWith {
            addHandler { request ->
                assertEquals("/api/tags", request.url.encodedPath)
                respond(
                    content = """{"models":[{"name":"llama2","model":"llama2:latest","modified_at":"2024-01-01T00:00:00Z","size":3800000000,"digest":"abc123"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }
        }
        val service = OllamaApiService(client)

        val models = service.getModels()

        assertEquals(1, models.size)
        assertEquals("llama2", models[0].name)
    }

    @Test
    fun `getModels throws OllamaApiException on server error`() = runTest {
        val client = clientWith {
            addHandler {
                respond("Internal error", HttpStatusCode.InternalServerError)
            }
        }
        val service = OllamaApiService(client)

        val exception = assertFailsWith<OllamaApiException> { service.getModels() }
        assertEquals(500, exception.statusCode)
        assertEquals("/api/tags", exception.endpoint)
    }

    @Test
    fun `deleteModel throws OllamaApiException on not found`() = runTest {
        val client = clientWith {
            addHandler { request ->
                assertEquals("/api/delete", request.url.encodedPath)
                respond("""{"error":"model 'x' not found"}""", HttpStatusCode.NotFound)
            }
        }
        val service = OllamaApiService(client)

        val exception = assertFailsWith<OllamaApiException> { service.deleteModel("x") }
        assertEquals(404, exception.statusCode)
        assertEquals("/api/delete", exception.endpoint)
    }

    @Test
    fun `deleteModel does not throw on success`() = runTest {
        val client = clientWith {
            addHandler { respond("", HttpStatusCode.OK) }
        }
        val service = OllamaApiService(client)

        service.deleteModel("llama2") // should not throw
    }

}