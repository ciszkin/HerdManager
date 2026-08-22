package by.ciszkin.herdmanager.data.api

import by.ciszkin.herdmanager.domain.model.GitHubRelease
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers

class GitHubApiService(private val client: HttpClient) {
    suspend fun getLatestRelease(): GitHubRelease {
        val response = client.get("https://api.github.com/repos/ollama/ollama/releases/latest") {
            headers {
                append("User-Agent", "Mozilla/5.0")
                append("Accept", "application/vnd.github+json")
            }
        }
        return response.body<GitHubRelease>()
    }
}
