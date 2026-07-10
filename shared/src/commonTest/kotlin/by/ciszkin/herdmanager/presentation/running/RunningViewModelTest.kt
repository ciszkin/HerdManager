package by.ciszkin.herdmanager.presentation.running

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.GetRunningModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class RunningViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testSettingsRepository: SettingsRepository
    private lateinit var testOllamaRepository: OllamaRepository
    private lateinit var observeSettingsUseCase: ObserveSettingsUseCase
    private lateinit var getRunningModelsUseCase: GetRunningModelsUseCase
    private lateinit var viewModel: RunningViewModel

    private val testSettings = Settings(
        serverUrl = "http://localhost:11434",
        refreshInterval = 5,
        pollingEnabled = true,
        language = "en",
        themeMode = ThemeMode.SYSTEM
    )

    private val testRunningModels = listOf(
        RunningModel(
            name = "llama2",
            model = "llama2:latest",
            size = 1024 * 1024 * 1024,
            digest = "abc123",
            details = null,
            expiresAt = null,
            sizeVram = null
        ),
        RunningModel(
            name = "mistral",
            model = "mistral:latest",
            size = 2048 * 1024 * 1024,
            digest = "def456",
            details = null,
            expiresAt = null,
            sizeVram = null
        )
    )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Create test settings repository with in-memory implementation
        testSettingsRepository = object : SettingsRepository {
            private val mutableSettingsFlow = MutableStateFlow(testSettings)
            override val settingsFlow: Flow<Settings> = mutableSettingsFlow

            override suspend fun saveSettings(settings: Settings): Result<Unit> {
                return runCatching {
                    mutableSettingsFlow.value = settings
                }
            }
        }

        // Create test Ollama repository
        testOllamaRepository = object : OllamaRepository {
            private val mutableModelsFlow = MutableStateFlow(testRunningModels)

            override suspend fun getModels(): Result<List<OllamaModel>> {
                return Result.failure(NotImplementedError("Not needed for this test"))
            }

            override suspend fun getRunningModels(): Result<List<RunningModel>> {
                return runCatching {
                    mutableModelsFlow.value
                }
            }

            override suspend fun deleteModel(name: String): Result<Unit> {
                return Result.failure(NotImplementedError("Not needed for this test"))
            }

            override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
                return emptyFlow()
            }
        }

        // Create use cases
        observeSettingsUseCase = ObserveSettingsUseCase(testSettingsRepository)

        getRunningModelsUseCase = GetRunningModelsUseCase(testOllamaRepository)

        viewModel = RunningViewModel(
            getRunningModelsUseCase = getRunningModelsUseCase,
            observeSettingsUseCase = observeSettingsUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state.value
        assertEquals(emptyList<RunningModel>(), state.models)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.pollingEnabled)
        assertEquals(0L, state.pollingIntervalMs)
    }

    @Test
    fun `Initialize intent starts observing settings and loads models`() = runTest {
        viewModel.onIntent(RunningIntent.Initialize)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.state.value
        assertTrue(state.pollingEnabled)
        assertEquals(5000L, state.pollingIntervalMs) // 5 seconds * 1000
        assertEquals(testRunningModels, state.models)
        assertFalse(state.isLoading)
        assertNull(state.error)

        viewModel.onIntent(RunningIntent.StopPolling)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `Initialize intent with polling disabled stops polling`() = runTest {
        // Update settings to disable polling
        val disabledSettings = testSettings.copy(pollingEnabled = false)
        val repositoryWithDisabledPolling = object : SettingsRepository {
            private val mutableSettingsFlow = MutableStateFlow(disabledSettings)
            override val settingsFlow: Flow<Settings> = mutableSettingsFlow

            override suspend fun saveSettings(settings: Settings): Result<Unit> {
                return runCatching {
                    mutableSettingsFlow.value = settings
                }
            }
        }

        val observeSettingsUseCase = ObserveSettingsUseCase(repositoryWithDisabledPolling)
        val viewModelWithDisabledPolling = RunningViewModel(
            getRunningModelsUseCase = getRunningModelsUseCase,
            observeSettingsUseCase = observeSettingsUseCase
        )

        viewModelWithDisabledPolling.onIntent(RunningIntent.Initialize)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModelWithDisabledPolling.state.value
        assertFalse(state.pollingEnabled)
    }

    @Test
    fun `Initialize intent observes settings with polling enabled`() = runTest {
        viewModel.onIntent(RunningIntent.Initialize)
        testDispatcher.scheduler.runCurrent()

        // Polling should be enabled based on test settings
        assertTrue(viewModel.state.value.pollingEnabled)
        assertEquals(5000L, viewModel.state.value.pollingIntervalMs)
        // Models should be loaded during initialization
        assertEquals(testRunningModels, viewModel.state.value.models)

        viewModel.onIntent(RunningIntent.StopPolling)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `Refresh intent loads running models successfully`() = runTest {
        viewModel.onIntent(RunningIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testRunningModels, state.models)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `Refresh intent sets loading state during fetch`() = runTest {
        // The shared in-memory repository returns synchronously, so under the unconfined test
        // dispatcher the whole refresh completes inline and isLoading is never observable.
        // Use a repository whose fetch suspends so the transient loading state is visible.
        val suspendingRepository = object : OllamaRepository {
            override suspend fun getModels(): Result<List<OllamaModel>> =
                Result.failure(NotImplementedError("Not needed for this test"))

            override suspend fun getRunningModels(): Result<List<RunningModel>> = runCatching {
                delay(1.milliseconds)
                testRunningModels
            }

            override suspend fun deleteModel(name: String): Result<Unit> =
                Result.failure(NotImplementedError("Not needed for this test"))

            override fun pullModel(modelName: String): Flow<Result<PullProgress>> =
                emptyFlow()
        }

        val loadingViewModel = RunningViewModel(
            getRunningModelsUseCase = GetRunningModelsUseCase(suspendingRepository),
            observeSettingsUseCase = observeSettingsUseCase
        )

        loadingViewModel.onIntent(RunningIntent.Refresh)

        // The fetch is suspended in delay(1), so isLoading should be true right after the intent
        val loadingState = loadingViewModel.state.value
        assertTrue(loadingState.isLoading)
        assertNull(loadingState.error)

        testDispatcher.scheduler.advanceUntilIdle()

        // Check final state
        val finalState = loadingViewModel.state.value
        assertEquals(testRunningModels, finalState.models)
        assertFalse(finalState.isLoading)
        assertNull(finalState.error)
    }

    @Test
    fun `Refresh intent handles error when loading models fails`() = runTest {
        // Create failing repository
        val failingRepository = object : OllamaRepository {
            override suspend fun getModels(): Result<List<OllamaModel>> {
                return Result.failure(NotImplementedError("Not needed for this test"))
            }

            override suspend fun getRunningModels(): Result<List<RunningModel>> {
                return Result.failure(Exception("Network error"))
            }

            override suspend fun deleteModel(name: String): Result<Unit> {
                return Result.failure(NotImplementedError("Not needed for this test"))
            }

            override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
                return emptyFlow()
            }
        }

        val failingUseCase = GetRunningModelsUseCase(failingRepository)

        val failingViewModel = RunningViewModel(
            getRunningModelsUseCase = failingUseCase,
            observeSettingsUseCase = observeSettingsUseCase
        )

        failingViewModel.onIntent(RunningIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        assertTrue(state.models.isEmpty())
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Network error", state.error.cause?.message)
    }

    @Test
    fun `StopPolling intent stops polling and updates state`() = runTest {
        // First initialize to start polling
        viewModel.onIntent(RunningIntent.Initialize)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.pollingEnabled)

        // Now stop polling
        viewModel.onIntent(RunningIntent.StopPolling)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.pollingEnabled)
    }

    @Test
    fun `multiple Refresh intents load models correctly`() = runTest {
        // First refresh
        viewModel.onIntent(RunningIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testRunningModels, viewModel.state.value.models)

        // Second refresh
        viewModel.onIntent(RunningIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testRunningModels, viewModel.state.value.models)
    }

    @Test
    fun `StopPolling intent can be called without Initialize`() = runTest {
        // Stop polling without initializing first
        viewModel.onIntent(RunningIntent.StopPolling)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.pollingEnabled)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.models.isEmpty())
    }
}
