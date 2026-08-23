package by.ciszkin.herdmanager.domain.usecase

import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetRegistryModelsUseCaseTest {

    private val testModels = listOf(
        RegistryModel(
            id = "llama3",
            name = "llama3",
            description = "Meta's Llama 3 model",
            pullCount = 1_000_000,
            tags = listOf("8b"),
            capabilities = listOf("chat"),
            updatedAt = "2024-01-15"
        )
    )

    private fun retryableFailure(): Result<List<RegistryModel>> =
        Result.failure(AppException(ConnectionError(cause = null, host = "ollama.com")))

    private fun nonRetryableFailure(): Result<List<RegistryModel>> =
        Result.failure(AppException(UnexpectedError(cause = null, context = "registry")))

    @Test
    fun `retries retryable network failures then succeeds`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = mockk<RegistryRepository>()
        var calls = 0
        coEvery { repository.getModels("qwen", 1, RegistrySort.POPULAR, null) } answers {
            calls++
            if (calls < 3) retryableFailure() else Result.success(testModels)
        }

        val useCase = GetRegistryModelsUseCase(repository, dispatcher)
        val result = useCase("qwen", 1)

        assertTrue(result.isSuccess)
        assertEquals(3, calls)
        coVerify(exactly = 3) { repository.getModels("qwen", 1, RegistrySort.POPULAR, null) }
    }

    @Test
    fun `does not retry non-retryable failures`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = mockk<RegistryRepository>()
        var calls = 0
        coEvery { repository.getModels("qwen", 1, RegistrySort.POPULAR, null) } answers {
            calls++
            nonRetryableFailure()
        }

        val useCase = GetRegistryModelsUseCase(repository, dispatcher)
        val result = useCase("qwen", 1)

        assertTrue(result.isFailure)
        assertEquals(1, calls, "Non-retryable failure must not be retried")
        coVerify(exactly = 1) { repository.getModels("qwen", 1, RegistrySort.POPULAR, null) }
    }
}