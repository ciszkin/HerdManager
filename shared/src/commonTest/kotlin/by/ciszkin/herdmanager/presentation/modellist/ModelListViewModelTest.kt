package by.ciszkin.herdmanager.presentation.modellist

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
class ModelListViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockGetModelsUseCase: GetModelsUseCase
    private lateinit var mockDeleteModelUseCase: DeleteModelUseCase
    private lateinit var viewModel: ModelListViewModel

    private val testModels = listOf(
        OllamaModel(
            name = "llama2",
            model = "llama2:latest",
            modifiedAt = "2024-01-01T00:00:00Z",
            size = 3_800_000_000,
            digest = "abc123"
        ),
        OllamaModel(
            name = "mistral",
            model = "mistral:latest",
            modifiedAt = "2024-01-02T00:00:00Z",
            size = 4_100_000_000,
            digest = "def456"
        )
    )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Create mock use cases
        mockGetModelsUseCase = mockk()
        mockDeleteModelUseCase = mockk()

        viewModel = ModelListViewModel(
            getModelsUseCase = mockGetModelsUseCase,
            deleteModelUseCase = mockDeleteModelUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state.value
        assertTrue(state.models.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isDeleting)
        assertNull(state.modelToDelete)
        assertNull(state.error)
    }

    @Test
    fun `Refresh intent loads models successfully`() = runTest {
        // Given
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)

        // When
        viewModel.onIntent(ModelListIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.models.size)
        assertEquals("llama2", state.models[0].name)
        assertEquals("mistral", state.models[1].name)
        assertNull(state.error)
        coVerify { mockGetModelsUseCase() }
    }

    @Test
    fun `Refresh intent handles loading state`() = runTest {
        // Given
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)

        // When
        viewModel.onIntent(ModelListIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - loading should be false after completion
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.models.size)
    }

    @Test
    fun `Refresh intent handles error`() = runTest {
        // Given
        val errorMessage = "Failed to connect to Ollama"
        coEvery { mockGetModelsUseCase() } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.onIntent(ModelListIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.models.isEmpty())
        assertNotNull(state.error)
        assertEquals(errorMessage, state.error.cause?.message)
    }

    @Test
    fun `Retry intent loads models successfully`() = runTest {
        // Given
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)

        // When
        viewModel.onIntent(ModelListIntent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.models.size)
        assertNull(state.error)
        coVerify { mockGetModelsUseCase() }
    }

    @Test
    fun `DeleteModel intent sends confirmation effect`() = runTest {
        // Given
        val modelName = "llama2"
        val emitted = mutableListOf<ModelListEffect>()
        val collectJob = launch(testDispatcher) { viewModel.effect.toList(emitted) }

        // When
        viewModel.onIntent(ModelListIntent.DeleteModel(modelName))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        collectJob.cancel()
        assertEquals(1, emitted.size)
        assertEquals(ModelListEffect.ShowDeleteConfirmation(modelName), emitted.first())
    }

    @Test
    fun `ConfirmDelete intent deletes model successfully`() = runTest {
        // Given
        val modelName = "llama2"
        coEvery { mockDeleteModelUseCase(modelName) } returns Result.success(Unit)
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)
        val emitted = mutableListOf<ModelListEffect>()
        val collectJob = launch(testDispatcher) { viewModel.effect.toList(emitted) }

        // When
        viewModel.onIntent(ModelListIntent.ConfirmDelete(modelName))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        collectJob.cancel()
        val state = viewModel.state.value
        assertFalse(state.isDeleting)
        assertNull(state.modelToDelete)

        // Verify delete was called
        coVerify { mockDeleteModelUseCase(modelName) }

        // Verify models were refreshed after deletion
        coVerify { mockGetModelsUseCase() }

        // Verify success effect was sent
        assertEquals(1, emitted.size)
        assertEquals(ModelListEffect.ShowModelDeletionSuccess, emitted.first())
    }

    @Test
    fun `ConfirmDelete intent handles deletion failure`() = runTest {
        // Given
        val modelName = "llama2"
        coEvery { mockDeleteModelUseCase(modelName) } returns Result.failure(Exception("Delete failed"))
        val emitted = mutableListOf<ModelListEffect>()
        val collectJob = launch(testDispatcher) { viewModel.effect.toList(emitted) }

        // When
        viewModel.onIntent(ModelListIntent.ConfirmDelete(modelName))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        collectJob.cancel()
        val state = viewModel.state.value
        assertFalse(state.isDeleting)
        assertNull(state.modelToDelete)

        // Verify delete was called
        coVerify { mockDeleteModelUseCase(modelName) }

        // Verify failure effect was sent
        assertEquals(1, emitted.size)
        assertEquals(ModelListEffect.ShowModelDeletionFailure, emitted.first())
    }

    @Test
    fun `ConfirmDelete intent handles deletion state correctly`() = runTest {
        // Given
        val modelName = "llama2"
        coEvery { mockDeleteModelUseCase(modelName) } returns Result.success(Unit)
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)

        // When
        viewModel.onIntent(ModelListIntent.ConfirmDelete(modelName))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - state should be reset after completion
        assertFalse(viewModel.state.value.isDeleting)
        assertNull(viewModel.state.value.modelToDelete)
    }

    @Test
    fun `Refresh intent clears previous error`() = runTest {
        // Given - first request fails
        val errorMessage = "Connection failed"
        coEvery { mockGetModelsUseCase() } returns Result.failure(Exception(errorMessage))

        viewModel.onIntent(ModelListIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        // When - second request succeeds
        coEvery { mockGetModelsUseCase() } returns Result.success(testModels)
        viewModel.onIntent(ModelListIntent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - error should be cleared
        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(2, state.models.size)
    }
}
