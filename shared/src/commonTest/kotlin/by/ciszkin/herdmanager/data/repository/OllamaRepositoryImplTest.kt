package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OllamaRepositoryImplTest {

    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockApiService: OllamaApiService
    private lateinit var repository: OllamaRepositoryImpl

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

    private val testRunningModels = listOf(
        RunningModel(
            name = "llama2",
            model = "llama2:latest",
            size = 3_800_000_000,
            digest = "abc123",
            expiresAt = "2024-01-01T01:00:00Z",
            sizeVram = 2_000_000_000
        ),
        RunningModel(
            name = "mistral",
            model = "mistral:latest",
            size = 4_100_000_000,
            digest = "def456"
        )
    )

    @BeforeTest
    fun setup() {
        // Create mocks
        mockConnectionManager = mockk()
        mockApiService = mockk()

        // Setup ConnectionManager to return mock API service and current URL
        every { mockConnectionManager.getApiService() } returns mockApiService
        every { mockConnectionManager.currentUrl } returns "localhost:11434"

        // Create repository
        repository = OllamaRepositoryImpl(mockConnectionManager)
    }

    @Test
    fun `getModels returns success result`() = runTest {
        // Given
        coEvery { mockApiService.getModels() } returns testModels

        // When
        val result = repository.getModels()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("llama2", result.getOrNull()?.get(0)?.name)
        assertEquals("mistral", result.getOrNull()?.get(1)?.name)
        coVerify { mockApiService.getModels() }
    }

    @Test
    fun `getModels returns failure result on exception`() = runTest {
        // Given
        val exception = RuntimeException("Connection failed")
        coEvery { mockApiService.getModels() } throws exception

        // When
        val result = repository.getModels()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals(exception, (result.exceptionOrNull() as AppException).cause)
        coVerify { mockApiService.getModels() }
    }

    @Test
    fun `getRunningModels returns success result`() = runTest {
        // Given
        coEvery { mockApiService.getRunningModels() } returns testRunningModels

        // When
        val result = repository.getRunningModels()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("llama2", result.getOrNull()?.get(0)?.name)
        assertEquals("mistral", result.getOrNull()?.get(1)?.name)
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `getRunningModels returns failure result on exception`() = runTest {
        // Given
        val exception = RuntimeException("Server unavailable")
        coEvery { mockApiService.getRunningModels() } throws exception

        // When
        val result = repository.getRunningModels()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals(exception, (result.exceptionOrNull() as AppException).cause)
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `getRunningModels returns empty list when no models running`() = runTest {
        // Given
        coEvery { mockApiService.getRunningModels() } returns emptyList()

        // When
        val result = repository.getRunningModels()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `deleteModel returns success result`() = runTest {
        // Given
        val modelName = "llama2"
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery { mockApiService.deleteModel(modelName) } returns mockHttpResponse

        // When
        val result = repository.deleteModel(modelName)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `deleteModel returns failure result on exception`() = runTest {
        // Given
        val modelName = "llama2"
        val exception = RuntimeException("Model not found")
        coEvery { mockApiService.deleteModel(modelName) } throws exception

        // When
        val result = repository.deleteModel(modelName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals(exception, (result.exceptionOrNull() as AppException).cause)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `deleteModel returns failure result on network error`() = runTest {
        // Given
        val modelName = "llama2"
        val exception = RuntimeException("Network timeout")
        coEvery { mockApiService.deleteModel(modelName) } throws exception

        // When
        val result = repository.deleteModel(modelName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        assertEquals("Unexpected error in deleteModel: Network timeout", result.exceptionOrNull()?.message)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with progress updates`() = runTest {
        // Given
        val modelName = "llama2"
        val progressUpdates = listOf(
            PullProgress(status = "pulling manifest"),
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 500000),
            PullProgress(status = "verifying", digest = "abc123", total = 1000000, completed = 1000000),
            PullProgress(status = "success")
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1]),
            Result.success(progressUpdates[2]),
            Result.success(progressUpdates[3])
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        // When
        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        // Then
        assertEquals(4, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("pulling manifest", results[0].getOrNull()?.status)
        assertEquals("downloading", results[1].getOrNull()?.status)
        assertEquals(500000, results[1].getOrNull()?.completed)
        assertEquals("verifying", results[2].getOrNull()?.status)
        assertEquals("success", results[3].getOrNull()?.status)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with error result`() = runTest {
        // Given
        val modelName = "llama2"
        val exception = RuntimeException("Download failed")
        val resultFlow = flowOf<Result<PullProgress>>(
            Result.success(PullProgress(status = "starting")),
            Result.failure(exception)
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        // When
        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("starting", results[0].getOrNull()?.status)
        assertTrue(results[1].isFailure)
        assertEquals(exception, results[1].exceptionOrNull())
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with error in PullProgress`() = runTest {
        // Given
        val modelName = "llama2"
        val errorProgress = PullProgress(error = "Authentication failed")
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "starting")),
            Result.success(errorProgress)
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        // When
        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("starting", results[0].getOrNull()?.status)
        assertTrue(results[1].isSuccess)
        assertEquals("Authentication failed", results[1].getOrNull()?.error)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with partial progress`() = runTest {
        // Given
        val modelName = "llama2"
        val progressUpdates = listOf(
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 250000),
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 500000),
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 750000)
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1]),
            Result.success(progressUpdates[2])
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        // When
        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        // Then
        assertEquals(3, results.size)
        assertEquals(250000, results[0].getOrNull()?.completed)
        assertEquals(500000, results[1].getOrNull()?.completed)
        assertEquals(750000, results[2].getOrNull()?.completed)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `getModels uses current API service from ConnectionManager`() = runTest {
        // Given
        coEvery { mockApiService.getModels() } returns testModels

        // When
        repository.getModels()

        // Then - verify getApiService was called
        verify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.getModels() }
    }

    @Test
    fun `getRunningModels uses current API service from ConnectionManager`() = runTest {
        // Given
        coEvery { mockApiService.getRunningModels() } returns testRunningModels

        // When
        repository.getRunningModels()

        // Then - verify getApiService was called
        verify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `deleteModel uses current API service from ConnectionManager`() = runTest {
        // Given
        val modelName = "llama2"
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery { mockApiService.deleteModel(modelName) } returns mockHttpResponse

        // When
        repository.deleteModel(modelName)

        // Then - verify getApiService was called
        verify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `pullModel uses current API service from ConnectionManager`() = runTest {
        // Given
        val modelName = "llama2"
        val resultFlow = flowOf(Result.success(PullProgress(status = "starting")))
        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        // When
        repository.pullModel(modelName).toList()

        // Then - verify getApiService was called
        verify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.pullModel(modelName) }
    }
}
