package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.data.api.GitHubApiService
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.local.currentVersionFlow
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.data.local.latestVersionFlow
import by.ciszkin.herdmanager.data.local.saveCurrentVersion
import by.ciszkin.herdmanager.data.local.saveLatestVersion
import by.ciszkin.herdmanager.domain.util.VersionComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class UpdateInfo(
    val currentVersion: String?,
    val latestVersion: String?,
    val isNewerAvailable: Boolean,
    val releaseUrl: String?
)

class CheckForOllamaUpdateUseCase(
    private val ollamaApi: OllamaApiService,
    private val githubApi: GitHubApiService
) {
    suspend fun checkForUpdates() {
        var currentVersion: String?
        var latestVersion: String?

        try {
            // Fetch current version from Ollama
            currentVersion = ollamaApi.getVersion()
            dataStore.saveCurrentVersion(currentVersion)
        } catch (e: Exception) {
            // Use cached value
        }

        try {
            // Fetch latest version from GitHub
            val release = githubApi.getLatestRelease()
            latestVersion = release.tagName
            dataStore.saveLatestVersion(latestVersion)
        } catch (e: Exception) {
            // Use cached value
        }
    }

    operator fun invoke(): Flow<UpdateInfo> {
        return combine(
            dataStore.currentVersionFlow(),
            dataStore.latestVersionFlow()
        ) { currentVersion, latestVersion ->
            val isNewerAvailable = VersionComparator.isNewerAvailable(currentVersion, latestVersion)
            UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                isNewerAvailable = isNewerAvailable,
                releaseUrl = if (isNewerAvailable && latestVersion != null) {
                    "https://github.com/ollama/ollama/releases/tag/$latestVersion"
                } else null
            )
        }
    }
}
