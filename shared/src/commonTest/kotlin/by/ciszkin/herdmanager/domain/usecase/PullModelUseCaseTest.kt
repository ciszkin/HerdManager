package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PullModelUseCaseTest {

    private lateinit var mockRepository: OllamaRepository
    private lateinit var pullModelUseCase: PullModelUseCase

    @BeforeTest
    fun setup() {
        mockRepository = mockk()
        pullModelUseCase = PullModelUseCase(mockRepository)
    }

    @Test
    fun `invoke transforms Flow of Result to Flow of PullResult`() = runTest {
        // Given
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

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(3, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue(results[1] is PullResult.Progress)
        assertTrue(results[2] is PullResult.Completed)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke maps pulling manifest status to Starting`() = runTest {
        // Given
        val modelName = "mistral"
        val progress = PullProgress(status = "pulling manifest")
        val resultFlow = flowOf(Result.success(progress))

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is PullResult.Starting)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke maps success status to Completed`() = runTest {
        // Given
        val modelName = "codellama"
        val progress = PullProgress(status = "success")
        val resultFlow = flowOf(Result.success(progress))

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is PullResult.Completed)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke maps other statuses to Progress`() = runTest {
        // Given
        val modelName = "neural-chat"
        val progressUpdates = listOf(
            PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 250000),
            PullProgress(status = "verifying", digest = "abc123", total = 1000000, completed = 1000000),
            PullProgress(status = "pulling 5f0e", digest = "def456", total = 2000000, completed = 0)
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1]),
            Result.success(progressUpdates[2])
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(3, results.size)
        assertTrue(results[0] is PullResult.Progress)
        assertTrue(results[1] is PullResult.Progress)
        assertTrue(results[2] is PullResult.Progress)

        assertEquals(progressUpdates[0], (results[0] as PullResult.Progress).progress)
        assertEquals(progressUpdates[1], (results[1] as PullResult.Progress).progress)
        assertEquals(progressUpdates[2], (results[2] as PullResult.Progress).progress)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke transforms error in PullProgress to Error result`() = runTest {
        // Given
        val modelName = "phi"
        val errorMessage = "Authentication failed"
        val errorProgress = PullProgress(error = errorMessage)
        val resultFlow = flowOf(Result.success(errorProgress))

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is PullResult.Error)
        assertTrue((results[0] as PullResult.Error).error is UnexpectedError)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke filters out failed Results`() = runTest {
        // Given
        val modelName = "gemma"
        val exception = RuntimeException("Network timeout")
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "pulling manifest")),
            Result.failure<PullProgress>(exception),
            Result.success(PullProgress(status = "success"))
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue(results[1] is PullResult.Completed)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke filters out null results from mapNotNull`() = runTest {
        // Given
        val modelName = "llama3"
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "pulling manifest")),
            Result.failure<PullProgress>(RuntimeException("Connection lost")),
            Result.success(PullProgress(status = "verifying")),
            Result.failure<PullProgress>(RuntimeException("Server error")),
            Result.success(PullProgress(status = "success"))
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(3, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue(results[1] is PullResult.Progress)
        assertTrue(results[2] is PullResult.Completed)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke handles empty flow`() = runTest {
        // Given
        val modelName = "tinyllama"
        val resultFlow = flowOf<Result<PullProgress>>()

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(0, results.size)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke transforms complete pull flow from start to finish`() = runTest {
        // Given
        val modelName = "qwen2"
        val progressUpdates = listOf(
            PullProgress(status = "pulling manifest"),
            PullProgress(status = "pulling abc123", digest = "abc123", total = 500000000, completed = 0),
            PullProgress(status = "pulling abc123", digest = "abc123", total = 500000000, completed = 250000000),
            PullProgress(status = "pulling abc123", digest = "abc123", total = 500000000, completed = 500000000),
            PullProgress(status = "verifying sha256 digest", digest = "abc123", total = 500000000, completed = 500000000),
            PullProgress(status = "writing manifest"),
            PullProgress(status = "success")
        )

        val resultFlow = flowOf(
            Result.success(progressUpdates[0]),
            Result.success(progressUpdates[1]),
            Result.success(progressUpdates[2]),
            Result.success(progressUpdates[3]),
            Result.success(progressUpdates[4]),
            Result.success(progressUpdates[5]),
            Result.success(progressUpdates[6])
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(7, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue(results[1] is PullResult.Progress)
        assertTrue(results[2] is PullResult.Progress)
        assertTrue(results[3] is PullResult.Progress)
        assertTrue(results[4] is PullResult.Progress)
        assertTrue(results[5] is PullResult.Progress)
        assertTrue(results[6] is PullResult.Completed)

        assertEquals(progressUpdates[1], (results[1] as PullResult.Progress).progress)
        assertEquals(progressUpdates[4], (results[4] as PullResult.Progress).progress)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke handles error during pull flow`() = runTest {
        // Given
        val modelName = "mistral"
        val errorMessage = "Download interrupted"
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "pulling manifest")),
            Result.success(PullProgress(status = "downloading", digest = "abc123", total = 1000000, completed = 500000)),
            Result.success(PullProgress(error = errorMessage))
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(3, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue(results[1] is PullResult.Progress)
        assertTrue(results[2] is PullResult.Error)
        assertTrue((results[2] as PullResult.Error).error is UnexpectedError)
        verify { mockRepository.pullModel(modelName) }
    }

    @Test
    fun `invoke handles multiple consecutive errors`() = runTest {
        // Given
        val modelName = "codellama"
        val resultFlow = flowOf(
            Result.success(PullProgress(status = "pulling manifest")),
            Result.success(PullProgress(error = "First error")),
            Result.success(PullProgress(status = "retrying")),
            Result.success(PullProgress(error = "Second error"))
        )

        coEvery { mockRepository.pullModel(modelName) } returns resultFlow

        // When
        val results = pullModelUseCase(modelName).toList()

        // Then
        assertEquals(4, results.size)
        assertTrue(results[0] is PullResult.Starting)
        assertTrue((results[1] as PullResult.Error).error is UnexpectedError)
        assertTrue(results[2] is PullResult.Progress)
        assertTrue((results[3] as PullResult.Error).error is UnexpectedError)
        verify { mockRepository.pullModel(modelName) }
    }
}
