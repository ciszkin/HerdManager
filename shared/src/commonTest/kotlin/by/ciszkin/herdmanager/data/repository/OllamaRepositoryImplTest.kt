package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.HttpError
import by.ciszkin.herdmanager.domain.error.OllamaApiException
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.RunningModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
        mockConnectionManager = mockk()
        mockApiService = mockk()

        coEvery { mockConnectionManager.getApiService() } returns mockApiService
        every { mockConnectionManager.currentUrl } returns "localhost:11434"

        repository = OllamaRepositoryImpl(mockConnectionManager)
    }

    @Test
    fun `getModels returns success result`() = runTest {
        coEvery { mockApiService.getModels() } returns testModels

        val result = repository.getModels()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("llama2", result.getOrNull()?.get(0)?.name)
        coVerify { mockApiService.getModels() }
    }

    @Test
    fun `getModels returns failure result on exception`() = runTest {
        val exception = RuntimeException("Connection failed")
        coEvery { mockApiService.getModels() } throws exception

        val result = repository.getModels()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        assertTrue((result.exceptionOrNull() as AppException).appError is UnexpectedError)
        coVerify { mockApiService.getModels() }
    }

    @Test
    fun `getRunningModels returns success result`() = runTest {
        coEvery { mockApiService.getRunningModels() } returns testRunningModels

        val result = repository.getRunningModels()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `getRunningModels returns failure result on exception`() = runTest {
        val exception = RuntimeException("Server unavailable")
        coEvery { mockApiService.getRunningModels() } throws exception

        val result = repository.getRunningModels()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `getRunningModels returns empty list when no models running`() = runTest {
        coEvery { mockApiService.getRunningModels() } returns emptyList()

        val result = repository.getRunningModels()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
        coVerify { mockApiService.getRunningModels() }
    }

    @Test
    fun `deleteModel returns success result`() = runTest {
        val modelName = "llama2"
        coEvery { mockApiService.deleteModel(modelName) } returns Unit

        val result = repository.deleteModel(modelName)

        assertTrue(result.isSuccess)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `deleteModel returns HttpError on OllamaApiException`() = runTest {
        val modelName = "llama2"
        val exception = OllamaApiException(404, "/api/delete")
        coEvery { mockApiService.deleteModel(modelName) } throws exception

        val result = repository.deleteModel(modelName)

        assertTrue(result.isFailure)
        val appException = result.exceptionOrNull() as AppException
        assertTrue(appException.appError is HttpError)
        assertEquals(404, (appException.appError as HttpError).statusCode)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `deleteModel returns failure on generic exception`() = runTest {
        val modelName = "llama2"
        val exception = RuntimeException("Network timeout")
        coEvery { mockApiService.deleteModel(modelName) } throws exception

        val result = repository.deleteModel(modelName)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
        coVerify { mockApiService.deleteModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with progress updates`() = runTest {
        val modelName = "llama2"
        val progressUpdates = listOf(
            PullProgress(status = "pulling manifest"),
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 500000),
            PullProgress(status = "success")
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1]),
            Result.success(progressUpdates[2])
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("pulling manifest", results[0].getOrNull()?.status)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with error result`() = runTest {
        val modelName = "llama2"
        val exception = RuntimeException("Download failed")
        val resultFlow = flowOf<Result<PullProgress>>(
            Result.success(PullProgress(status = "starting")),
            Result.failure(exception)
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isFailure)
        assertEquals(exception, results[1].exceptionOrNull())
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with error in PullProgress`() = runTest {
        val modelName = "llama2"
        val errorProgress = PullProgress(error = "Authentication failed")
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "starting")),
            Result.success(errorProgress)
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("starting", results[0].getOrNull()?.status)
        assertTrue(results[1].isSuccess)
        assertEquals("Authentication failed", results[1].getOrNull()?.error)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel returns flow with partial progress`() = runTest {
        val modelName = "llama2"
        val progressUpdates = listOf(
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 250000),
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 750000)
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1])
        )

        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        val flow = repository.pullModel(modelName)
        val results = flow.toList()

        assertEquals(2, results.size)
        assertEquals(250000, results[0].getOrNull()?.completed)
        assertEquals(750000, results[1].getOrNull()?.completed)
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel uses suspend getApiService inside the flow`() = runTest {
        val modelName = "llama2"
        val resultFlow = flowOf(Result.success(PullProgress(status = "starting")))
        coEvery { mockApiService.pullModel(modelName) } returns resultFlow

        repository.pullModel(modelName).toList()

        coVerify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.pullModel(modelName) }
    }

    @Test
    fun `pullModel propagates init failure so ViewModel catch can handle it`() = runTest {
        val modelName = "llama2"
        coEvery { mockConnectionManager.getApiService() } throws IllegalStateException("not started")

        val result = runCatching { repository.pullModel(modelName).toList() }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `getModels calls suspend getApiService`() = runTest {
        coEvery { mockApiService.getModels() } returns testModels

        repository.getModels()

        coVerify { mockConnectionManager.getApiService() }
        coVerify { mockApiService.getModels() }
    }
}
