package by.ciszkin.herdmanager.presentation.settings

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.CheckForOllamaUpdateUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.domain.usecase.SaveSettingsUseCase
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testRepository: SettingsRepository
    private lateinit var observeSettingsUseCase: ObserveSettingsUseCase
    private lateinit var saveSettingsUseCase: SaveSettingsUseCase
    private lateinit var viewModel: SettingsViewModel

    private val testSettings = Settings(
        serverUrl = "http://localhost:11434",
        refreshInterval = 5,
        pollingEnabled = true,
        language = "en",
        themeMode = ThemeMode.SYSTEM
    )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Create test repository with in-memory implementation
        testRepository = object : SettingsRepository {
            private val mutableSettingsFlow = MutableStateFlow(testSettings)
            override val settingsFlow: Flow<Settings> = mutableSettingsFlow

            override suspend fun saveSettings(settings: Settings): Result<Unit> {
                return runCatching {
                    mutableSettingsFlow.value = settings
                }
            }
        }

        // Create use cases
        observeSettingsUseCase = ObserveSettingsUseCase(testRepository)

        saveSettingsUseCase = SaveSettingsUseCase(testRepository, testDispatcher)

        // Mock the update use case so no real APIs or DataStore are touched
        val stubCheckForUpdatesUseCase = createUpdateUseCaseStub()

        viewModel = SettingsViewModel(
            observeSettingsUseCase = observeSettingsUseCase,
            saveSettingsUseCase = saveSettingsUseCase,
            checkForOllamaUpdateUseCase = stubCheckForUpdatesUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createUpdateUseCaseStub(): CheckForOllamaUpdateUseCase {
        val stub = mockk<CheckForOllamaUpdateUseCase>()
        every { stub.invoke() } returns emptyFlow()
        coEvery { stub.refreshIfDue() } returns true
        return stub
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertFalse(state.isDirty)
        assertNull(state.saveError)
        assertNull(state.settings)
        assertFalse(state.isNewVersionAvailable)
        assertNull(state.currentVersion)
        assertNull(state.latestVersion)
        assertNull(state.releaseUrl)
    }

    @Test
    fun `LoadSettings intent loads settings successfully`() = runTest {
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.settings)
        assertEquals(testSettings.serverUrl, state.settings.serverUrl)
        assertEquals(testSettings.refreshInterval, state.settings.refreshInterval)
        assertEquals(testSettings.pollingEnabled, state.settings.pollingEnabled)
        assertEquals(testSettings.language, state.settings.language)
        assertEquals(testSettings.themeMode, state.settings.themeMode)
        assertFalse(state.isDirty)
    }

    @Test
    fun `UpdateServerUrl intent updates URL and tracks dirty state`() = runTest {
        // First load settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update server URL
        val newUrl = "http://192.168.1.100:11434"
        viewModel.onIntent(SettingsIntent.UpdateServerUrl(newUrl))

        val state = viewModel.state.value
        assertEquals(newUrl, state.settings?.serverUrl)
        assertTrue(state.isDirty)
    }

    @Test
    fun `UpdateRefreshInterval intent updates interval and tracks dirty state`() = runTest {
        // First load settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update refresh interval
        val newInterval = 10
        viewModel.onIntent(SettingsIntent.UpdateRefreshInterval(newInterval))

        val state = viewModel.state.value
        assertEquals(newInterval, state.settings?.refreshInterval)
        assertTrue(state.isDirty)
    }

    @Test
    fun `UpdatePollingEnabled intent updates polling state and tracks dirty`() = runTest {
        // First load settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update polling enabled
        viewModel.onIntent(SettingsIntent.UpdatePollingEnabled(false))

        val state = viewModel.state.value
        assertFalse(state.settings?.pollingEnabled ?: true)
        assertTrue(state.isDirty)
    }

    @Test
    fun `UpdateLanguage intent updates language and tracks dirty state`() = runTest {
        // First load settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update language
        val newLanguage = "de"
        viewModel.onIntent(SettingsIntent.UpdateLanguage(newLanguage))

        val state = viewModel.state.value
        assertEquals(newLanguage, state.settings?.language)
        assertTrue(state.isDirty)
    }

    @Test
    fun `UpdateThemeMode intent updates theme and tracks dirty state`() = runTest {
        // First load settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update theme mode
        viewModel.onIntent(SettingsIntent.UpdateThemeMode(ThemeMode.DARK))

        val state = viewModel.state.value
        assertEquals(ThemeMode.DARK, state.settings?.themeMode)
        assertTrue(state.isDirty)
    }

    @Test
    fun `SaveSettings intent saves settings successfully`() = runTest {
        // Load and modify settings
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSettings = testSettings.copy(
            serverUrl = "http://192.168.1.100:11434",
            refreshInterval = 10
        )

        viewModel.onIntent(SettingsIntent.SaveSettings(updatedSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertNull(state.saveError)
        assertFalse(state.isDirty)
    }

    @Test
    fun `SaveSettings intent handles save failure`() = runTest {
        // Create failing repository
        val failingRepository = object : SettingsRepository {
            override val settingsFlow = MutableStateFlow(testSettings)
            override suspend fun saveSettings(settings: Settings): Result<Unit> {
                return Result.failure(Exception("Save failed"))
            }
        }

        val failingSaveUseCase = SaveSettingsUseCase(failingRepository, testDispatcher)

        val checkForUpdatesUseCase = createUpdateUseCaseStub()

        val failingViewModel = SettingsViewModel(
            observeSettingsUseCase = observeSettingsUseCase,
            saveSettingsUseCase = failingSaveUseCase,
            checkForOllamaUpdateUseCase = checkForUpdatesUseCase
        )

        // Load settings
        failingViewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Make settings dirty so we can verify a failed save leaves isDirty intact
        failingViewModel.onIntent(SettingsIntent.UpdateServerUrl("http://changed:11434"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempt to save
        failingViewModel.onIntent(SettingsIntent.SaveSettings(testSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        assertFalse(state.isSaving)
        assertNotNull(state.saveError)
        assertEquals("Save failed", state.saveError.cause?.message)
        assertTrue(state.isDirty) // Should remain dirty after failed save
    }

    @Test
    fun `ResetToDefaults intent resets to default settings`() = runTest {
        // Load settings first
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Modify settings
        viewModel.onIntent(SettingsIntent.UpdateServerUrl("http://custom:11434"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Reset to defaults
        viewModel.onIntent(SettingsIntent.ResetToDefaults)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("http://localhost:11434", state.settings?.serverUrl)
        assertEquals(5, state.settings?.refreshInterval)
        assertTrue(state.settings?.pollingEnabled ?: false)
        assertEquals("en", state.settings.language)
        assertEquals(ThemeMode.SYSTEM, state.settings.themeMode)
        // Defaults match the originally-loaded settings, so the screen is not dirty
        assertFalse(state.isDirty)
    }

    @Test
    fun `DiscardChanges intent discards unsaved changes`() = runTest {
        // Load settings first
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Modify settings
        viewModel.onIntent(SettingsIntent.UpdateServerUrl("http://custom:11434"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isDirty)

        // Discard changes
        viewModel.onIntent(SettingsIntent.DiscardChanges)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testSettings.serverUrl, state.settings?.serverUrl)
        assertFalse(state.isDirty)
    }

    @Test
    fun `multiple updates track dirty state correctly`() = runTest {
        // Load settings first
        viewModel.onIntent(SettingsIntent.LoadSettings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Make multiple changes
        viewModel.onIntent(SettingsIntent.UpdateServerUrl("http://custom:11434"))
        viewModel.onIntent(SettingsIntent.UpdateRefreshInterval(10))
        viewModel.onIntent(SettingsIntent.UpdateThemeMode(ThemeMode.DARK))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isDirty)

        // Discard all changes
        viewModel.onIntent(SettingsIntent.DiscardChanges)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testSettings.serverUrl, state.settings?.serverUrl)
        assertEquals(testSettings.refreshInterval, state.settings?.refreshInterval)
        assertEquals(testSettings.themeMode, state.settings?.themeMode)
        assertFalse(state.isDirty)
    }
}
