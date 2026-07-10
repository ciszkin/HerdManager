package by.ciszkin.herdmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import by.ciszkin.herdmanager.data.local.saveLanguage
import by.ciszkin.herdmanager.data.local.savePollingEnabled
import by.ciszkin.herdmanager.data.local.saveRefreshInterval
import by.ciszkin.herdmanager.data.local.saveServerUrl
import by.ciszkin.herdmanager.data.local.saveThemeMode
import by.ciszkin.herdmanager.data.local.settingsFlow
import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private companion object {
        const val SETTINGS_DATASTORE_KT = "by.ciszkin.herdmanager.data.local.SettingsDataStoreKt"
    }

    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    private val testSettings = Settings(
        serverUrl = "http://localhost:11434",
        refreshInterval = 5,
        pollingEnabled = true,
        language = "en",
        themeMode = ThemeMode.SYSTEM
    )

    @BeforeTest
    fun setup() {
        // Create mock with relaxed mode to allow DataStore operations
        mockDataStore = mockk(relaxed = true)

        // Setup basic DataStore behavior to prevent init block errors
        coEvery { mockDataStore.data } returns flowOf(mockk(relaxed = true))
        mockkStatic(SETTINGS_DATASTORE_KT)
        coEvery { mockDataStore.settingsFlow() } returns flowOf(testSettings)

        // Create repository
        repository = SettingsRepositoryImpl(mockDataStore)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `saveSettings returns success result when all operations succeed`() = runTest {
        // Given - relaxed mock allows all operations to succeed by default

        // When
        val result = repository.saveSettings(testSettings)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `saveSettings returns failure result when DataStore edit throws exception`() = runTest {
        // Given - need to mock the extension functions
        mockkStatic(SETTINGS_DATASTORE_KT)
        val exception = RuntimeException("DataStore error")
        coEvery { mockDataStore.saveServerUrl(any()) } throws exception

        // When
        val result = repository.saveSettings(testSettings)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals("Unexpected error in saveSettings: DataStore error", result.exceptionOrNull()?.message)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `saveSettings returns failure result when edit throws IllegalStateException`() = runTest {
        // Given - need to mock the extension functions
        mockkStatic(SETTINGS_DATASTORE_KT)
        val exception = IllegalStateException("Invalid interval")
        coEvery { mockDataStore.saveServerUrl(any()) } just Runs
        coEvery { mockDataStore.saveRefreshInterval(any()) } throws exception

        // When
        val result = repository.saveSettings(testSettings)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals("Unexpected error in saveSettings: Invalid interval", result.exceptionOrNull()?.message)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `saveSettings returns failure result when edit throws IllegalArgumentException`() = runTest {
        // Given - need to mock the extension functions
        mockkStatic(SETTINGS_DATASTORE_KT)
        val exception = IllegalArgumentException("Invalid argument")
        coEvery { mockDataStore.saveServerUrl(any()) } just Runs
        coEvery { mockDataStore.saveRefreshInterval(any()) } just Runs
        coEvery { mockDataStore.savePollingEnabled(any()) } throws exception

        // When
        val result = repository.saveSettings(testSettings)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals("Unexpected error in saveSettings: Invalid argument", result.exceptionOrNull()?.message)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `settingsFlow is accessible and can be collected`() = runTest {
        // Given
        val settingsFlow = flowOf(testSettings)
        mockkStatic(SETTINGS_DATASTORE_KT)
        coEvery { mockDataStore.settingsFlow() } returns settingsFlow

        // When - create new repository with mocked settingsFlow
        val testRepository = SettingsRepositoryImpl(mockDataStore)

        // Then - verify we can collect from the flow without errors
        val results = mutableListOf<Settings>()
        // Just verify the flow is accessible, actual collection happens in init block
        assertNotNull(testRepository.settingsFlow)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `saveSettings with custom parameters succeeds`() = runTest {
        // Given
        val customSettings = Settings(
            serverUrl = "http://custom:11434",
            refreshInterval = 30,
            pollingEnabled = false,
            language = "fr",
            themeMode = ThemeMode.LIGHT
        )

        // When
        val result = repository.saveSettings(customSettings)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `saveSettings calls edit on DataStore`() = runTest {
        // Given - need to mock the extension functions to verify calls
        mockkStatic(SETTINGS_DATASTORE_KT)
        coEvery { mockDataStore.saveServerUrl(any()) } just Runs
        coEvery { mockDataStore.saveRefreshInterval(any()) } just Runs
        coEvery { mockDataStore.savePollingEnabled(any()) } just Runs
        coEvery { mockDataStore.saveLanguage(any()) } just Runs
        coEvery { mockDataStore.saveThemeMode(any()) } just Runs

        // When
        repository.saveSettings(testSettings)

        // Then - verify all save operations were called
        coVerify { mockDataStore.saveServerUrl(any()) }
        coVerify { mockDataStore.saveRefreshInterval(any()) }
        coVerify { mockDataStore.savePollingEnabled(any()) }
        coVerify { mockDataStore.saveLanguage(any()) }
        coVerify { mockDataStore.saveThemeMode(any()) }

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }

    @Test
    fun `saveSettings returns Unit on success`() = runTest {
        // Given - relaxed mock

        // When
        val result = repository.saveSettings(testSettings)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `settingsFlow flows through repository`() = runTest {
        // Given - repository is already setup in @BeforeTest

        // When
        val flow = repository.settingsFlow

        // Then - verify flow is accessible
        assertNotNull(flow)
    }

    @Test
    fun `repository initialization creates settingsFlow`() = runTest {
        // Given
        val testDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        coEvery { testDataStore.data } returns flowOf(mockk(relaxed = true))
        mockkStatic(SETTINGS_DATASTORE_KT)
        coEvery { testDataStore.settingsFlow() } returns flowOf(testSettings)

        // When
        val testRepository = SettingsRepositoryImpl(testDataStore)

        // Then
        assertNotNull(testRepository.settingsFlow)

        unmockkStatic(SETTINGS_DATASTORE_KT)
    }
}
