package by.ciszkin.herdmanager.data.repository

import by.ciszkin.herdmanager.data.scraping.OllamaLibraryScraper
import by.ciszkin.herdmanager.domain.error.AppException
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.model.RegistryModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegistryRepositoryImplTest {

    private lateinit var repository: RegistryRepositoryImpl

    private val testModels = listOf(
        RegistryModel(
            id = "llama3",
            name = "Llama 3",
            description = "Meta's Llama 3 model",
            pullCount = 1_000_000,
            tags = listOf("8b", "70b"),
            capabilities = listOf("chat"),
            updatedAt = "2024-01-15"
        ),
        RegistryModel(
            id = "mistral",
            name = "Mistral",
            description = "Mistral AI model",
            pullCount = 500_000,
            tags = listOf("7b"),
            capabilities = listOf("chat"),
            updatedAt = "2024-01-10"
        )
    )

    @BeforeTest
    fun setup() {
        // Mock the scraper singleton so no real HTTP is performed and no Koin/HttpClient
        // setup is required (the repository delegates straight to fetchModels).
        mockkObject(OllamaLibraryScraper)
        repository = RegistryRepositoryImpl()
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(OllamaLibraryScraper)
    }

    @Test
    fun `getModels returns success with the models from the scraper`() = runTest {
        coEvery { OllamaLibraryScraper.fetchModels(any(), any()) } returns Result.success(testModels)

        val result = repository.getModels("llama", 1)

        assertTrue(result.isSuccess)
        assertEquals(testModels, result.getOrNull())
    }

    @Test
    fun `getModels maps a scraper failure into an AppException`() = runTest {
        coEvery { OllamaLibraryScraper.fetchModels(any(), any()) } returns
            Result.failure(RuntimeException("Network error"))

        val result = repository.getModels("llama", 1)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppException)
        assertTrue(error.appError is UnexpectedError)
    }

    @Test
    fun `getModels forwards query and page to the scraper`() = runTest {
        coEvery { OllamaLibraryScraper.fetchModels(any(), any()) } returns Result.success(emptyList())

        repository.getModels("mistral", 3)

        coVerify { OllamaLibraryScraper.fetchModels("mistral", 3) }
    }

    @Test
    fun `getModels with empty query still delegates to the scraper`() = runTest {
        coEvery { OllamaLibraryScraper.fetchModels(any(), any()) } returns Result.success(testModels)

        val result = repository.getModels("", 1)

        assertTrue(result.isSuccess)
        coVerify { OllamaLibraryScraper.fetchModels("", 1) }
    }
}
