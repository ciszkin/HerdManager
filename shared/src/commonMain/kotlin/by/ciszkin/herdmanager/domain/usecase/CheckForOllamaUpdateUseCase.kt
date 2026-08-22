package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.data.api.GitHubApiService
import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.local.currentVersionFlow
import by.ciszkin.herdmanager.data.local.dataStore
import by.ciszkin.herdmanager.data.local.lastUpdateCheckFlow
import by.ciszkin.herdmanager.data.local.latestVersionFlow
import by.ciszkin.herdmanager.data.local.saveCurrentVersion
import by.ciszkin.herdmanager.data.local.saveLastUpdateCheck
import by.ciszkin.herdmanager.data.local.saveLatestVersion
import by.ciszkin.herdmanager.domain.util.VersionComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

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
    /**
     * Runs a network check only if the last check is older than
     * [CHECK_TTL_MILLIS], throttling the unauthenticated GitHub API requests.
     * Returns true when a check actually ran.
     */
    suspend fun refreshIfDue(): Boolean {
        val lastCheck = dataStore.lastUpdateCheckFlow().first()
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastCheck < CHECK_TTL_MILLIS) return false
        checkForUpdates()
        dataStore.saveLastUpdateCheck(now)
        return true
    }

    suspend fun checkForUpdates() {
        try {
            val currentVersion = ollamaApi.getVersion()
            dataStore.saveCurrentVersion(currentVersion)
        } catch (e: Exception) {
            // Use cached value
        }

        try {
            val release = githubApi.getLatestRelease()
            dataStore.saveLatestVersion(release.tagName)
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

    companion object {
        private const val CHECK_TTL_MILLIS = 24L * 60 * 60 * 1000
    }
}