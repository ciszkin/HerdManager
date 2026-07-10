package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.data.api.GitHubApiService
import by.ciszkin.herdmanager.data.api.OllamaApiService
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CheckForOllamaUpdateUseCase.
 *
 * Note: Tests for checkForUpdates() are skipped in unit tests because they require
 * DataStore platform-specific initialization. These should be tested as integration tests.
 * This test class focuses on testing the UpdateInfo data model and invoke() logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckForOllamaUpdateUseCaseTest {

    private lateinit var mockOllamaApi: OllamaApiService
    private lateinit var mockGithubApi: GitHubApiService
    private lateinit var checkForOllamaUpdateUseCase: CheckForOllamaUpdateUseCase

    @BeforeTest
    fun setup() {
        mockOllamaApi = mockk(relaxed = true)
        mockGithubApi = mockk(relaxed = true)

        checkForOllamaUpdateUseCase = CheckForOllamaUpdateUseCase(mockOllamaApi, mockGithubApi)
    }

    @Test
    fun `UpdateInfo data class has correct structure`() = runTest {
        // Given
        val currentVersion = "0.1.30"
        val latestVersion = "0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = true,
            releaseUrl = "https://github.com/ollama/ollama/releases/tag/$latestVersion"
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertTrue(updateInfo.isNewerAvailable)
        assertEquals("https://github.com/ollama/ollama/releases/tag/$latestVersion", updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles null values correctly`() = runTest {
        // When
        val updateInfo = UpdateInfo(
            currentVersion = null,
            latestVersion = null,
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertNull(updateInfo.currentVersion)
        assertNull(updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles equal versions`() = runTest {
        // Given
        val currentVersion = "0.1.32"
        val latestVersion = "0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles current version newer than latest`() = runTest {
        // Given
        val currentVersion = "0.1.35"
        val latestVersion = "0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles newer version available`() = runTest {
        // Given
        val currentVersion = "0.1.30"
        val latestVersion = "0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = true,
            releaseUrl = "https://github.com/ollama/ollama/releases/tag/$latestVersion"
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertTrue(updateInfo.isNewerAvailable)
        assertEquals("https://github.com/ollama/ollama/releases/tag/$latestVersion", updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles current version only`() = runTest {
        // Given
        val currentVersion = "0.1.30"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = null,
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertNull(updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles latest version only`() = runTest {
        // Given
        val latestVersion = "0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = null,
            latestVersion = latestVersion,
            isNewerAvailable = true,
            releaseUrl = "https://github.com/ollama/ollama/releases/tag/$latestVersion"
        )

        // Then
        assertNull(updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertTrue(updateInfo.isNewerAvailable)
        assertEquals("https://github.com/ollama/ollama/releases/tag/$latestVersion", updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class handles version with v prefix correctly`() = runTest {
        // Given
        val currentVersion = "v0.1.30"
        val latestVersion = "v0.1.32"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = true,
            releaseUrl = "https://github.com/ollama/ollama/releases/tag/$latestVersion"
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertTrue(updateInfo.isNewerAvailable)
    }

    @Test
    fun `UpdateInfo data class handles version comparison with different formats`() = runTest {
        // Given
        val currentVersion = "0.1.30"
        val latestVersion = "0.2.0"

        // When
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isNewerAvailable = true,
            releaseUrl = "https://github.com/ollama/ollama/releases/tag/$latestVersion"
        )

        // Then
        assertEquals(currentVersion, updateInfo.currentVersion)
        assertEquals(latestVersion, updateInfo.latestVersion)
        assertTrue(updateInfo.isNewerAvailable)
    }

    @Test
    fun `UpdateInfo data class handles empty version strings`() = runTest {
        // When
        val updateInfo = UpdateInfo(
            currentVersion = "",
            latestVersion = "",
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertEquals("", updateInfo.currentVersion)
        assertEquals("", updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }

    @Test
    fun `UpdateInfo data class returns null versions when no data available`() = runTest {
        // When
        val updateInfo = UpdateInfo(
            currentVersion = null,
            latestVersion = null,
            isNewerAvailable = false,
            releaseUrl = null
        )

        // Then
        assertNull(updateInfo.currentVersion)
        assertNull(updateInfo.latestVersion)
        assertFalse(updateInfo.isNewerAvailable)
        assertNull(updateInfo.releaseUrl)
    }
}
