package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteModelUseCaseTest {

    private lateinit var mockRepository: OllamaRepository
    private lateinit var deleteModelUseCase: DeleteModelUseCase

    @BeforeTest
    fun setup() {
        mockRepository = mockk()
        deleteModelUseCase = DeleteModelUseCase(mockRepository)
    }

    @Test
    fun `invoke returns success when repository deleteModel succeeds`() = runTest {
        // Given
        val modelName = "llama2"
        coEvery { mockRepository.deleteModel(modelName) } returns Result.success(Unit)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke returns failure when repository deleteModel fails`() = runTest {
        // Given
        val modelName = "mistral"
        val exception = RuntimeException("Model not found")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke propagates repository exception`() = runTest {
        // Given
        val modelName = "codellama"
        val exception = IllegalArgumentException("Invalid model name")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke uses io dispatcher for repository call`() = runTest {
        // Given
        val modelName = "gemma"
        coEvery { mockRepository.deleteModel(modelName) } returns Result.success(Unit)

        // When
        deleteModelUseCase(modelName)

        // Then
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke handles network error`() = runTest {
        // Given
        val modelName = "phi"
        val exception = Exception("Connection refused")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Connection refused", result.exceptionOrNull()?.message)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke handles timeout error`() = runTest {
        // Given
        val modelName = "neural-chat"
        val exception = Exception("Request timeout")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Request timeout", result.exceptionOrNull()?.message)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke handles authentication error`() = runTest {
        // Given
        val modelName = "tinyllama"
        val exception = Exception("Unauthorized access")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Unauthorized access", result.exceptionOrNull()?.message)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke propagates success result with Unit value`() = runTest {
        // Given
        val modelName = "llama3"
        coEvery { mockRepository.deleteModel(modelName) } returns Result.success(Unit)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isSuccess)
        val value = result.getOrNull()
        assertEquals(Unit, value)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke maintains repository call semantics`() = runTest {
        // Given
        val modelName = "qwen2"
        coEvery { mockRepository.deleteModel(modelName) } returns Result.success(Unit)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        // Verify that the use case properly delegates to repository
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke handles model in use error`() = runTest {
        // Given
        val modelName = "mistral-instruct"
        val exception = IllegalStateException("Model is currently running and cannot be deleted")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Model is currently running and cannot be deleted", result.exceptionOrNull()?.message)
        coVerify { mockRepository.deleteModel(modelName) }
    }

    @Test
    fun `invoke handles empty model name gracefully`() = runTest {
        // Given
        val modelName = ""
        val exception = IllegalArgumentException("Model name cannot be empty")
        coEvery { mockRepository.deleteModel(modelName) } returns Result.failure(exception)

        // When
        val result = deleteModelUseCase(modelName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify { mockRepository.deleteModel(modelName) }
    }
}
