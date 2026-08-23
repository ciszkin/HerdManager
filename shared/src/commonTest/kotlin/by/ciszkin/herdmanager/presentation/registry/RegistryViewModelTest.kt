package by.ciszkin.herdmanager.presentation.registry

import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import by.ciszkin.herdmanager.domain.repository.RegistryRepository
import by.ciszkin.herdmanager.domain.repository.OllamaRepository
import by.ciszkin.herdmanager.domain.model.PullProgress
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.domain.model.RunningModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class RegistryViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testRegistryRepository: RegistryRepository
    private lateinit var testOllamaRepository: OllamaRepository
    private lateinit var getRegistryModelsUseCase: GetRegistryModelsUseCase
    private lateinit var pullModelUseCase: PullModelUseCase
    private lateinit var viewModel: RegistryViewModel

    private var requestedSort: RegistrySort? = null
    private var requestedCategory: String? = null
    private var fetchCount = 0

    private val testModels = listOf(
        RegistryModel(
            id = "llama3",
            name = "llama3",
            description = "Latest Llama 3 model",
            pullCount = 1000000,
            tags = listOf("latest", "8b", "70b"),
            capabilities = listOf("chat", "instruct"),
            updatedAt = "2024-01-15"
        ),
        RegistryModel(
            id = "mistral",
            name = "mistral",
            description = "Mistral AI model",
            pullCount = 500000,
            tags = listOf("latest", "7b"),
            capabilities = listOf("chat"),
            updatedAt = "2024-01-10"
        )
    )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Create test registry repository
        testRegistryRepository = object : RegistryRepository {
            override suspend fun getModels(
                query: String,
                page: Int,
                sort: RegistrySort,
                category: String?
            ): Result<List<RegistryModel>> {
                fetchCount++
                requestedSort = sort
                requestedCategory = category

                // Simulate pagination - return empty list for page > 1
                return if (page > 1) {
                    Result.success(emptyList())
                } else {
                    // Filter by query if provided
                    val filteredModels = if (query.isNotEmpty()) {
                        testModels.filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                        }
                    } else {
                        testModels
                    }
                    // Filter by capability if a category is selected
                    val categoryFiltered = if (category != null) {
                        filteredModels.filter { it.capabilities.contains(category) }
                    } else {
                        filteredModels
                    }
                    Result.success(categoryFiltered)
                }
            }
        }

        // Create test Ollama repository
        testOllamaRepository = object : OllamaRepository {
            override suspend fun getModels(): Result<List<OllamaModel>> {
                return Result.success(emptyList())
            }

            override suspend fun getRunningModels(): Result<List<RunningModel>> {
                return Result.success(emptyList())
            }

            override suspend fun deleteModel(name: String): Result<Unit> {
                return Result.success(Unit)
            }

            override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
                return flowOf(
                    Result.success(PullProgress(status = "pulling manifest", completed = 0, total = 100)),
                    Result.success(PullProgress(status = "success", completed = 100, total = 100))
                )
            }
        }

        // Create use cases
        getRegistryModelsUseCase = GetRegistryModelsUseCase(testRegistryRepository, testDispatcher)
        pullModelUseCase = PullModelUseCase(testOllamaRepository)

        viewModel = RegistryViewModel(getRegistryModelsUseCase, pullModelUseCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state.value
        assertTrue(state.models.isEmpty())
        assertEquals("", state.searchQuery)
        assertFalse(state.isLoading)
        assertFalse(state.isSearching)
        assertFalse(state.isLoadingMore)
        assertTrue(state.canLoadMore)
        assertEquals(1, state.currentPage)
        assertNull(state.error)
        assertNull(state.pullModelName)
        assertNull(state.pullResult)
        assertNull(state.selectedTag)
        assertTrue(state.availableTags.isEmpty())
    }

    @Test
    fun `LoadModels intent loads models successfully`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(testModels.size, state.models.size)
        assertEquals(testModels[0].id, state.models[0].id)
        assertEquals(testModels[0].name, state.models[0].name)
        assertTrue(state.canLoadMore)
        assertNull(state.error)
    }

    @Test
    fun `LoadModels intent handles loading failure`() = runTest {
        // Create failing repository
        val failingRepository = object : RegistryRepository {
            override suspend fun getModels(query: String, page: Int, sort: RegistrySort, category: String?): Result<List<RegistryModel>> {
                return Result.failure(Exception("Network error"))
            }
        }

        val failingUseCase = GetRegistryModelsUseCase(failingRepository, testDispatcher)

        val failingViewModel = RegistryViewModel(failingUseCase, pullModelUseCase)

        failingViewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Network error", state.error.cause?.message)
        assertTrue(state.models.isEmpty())
    }

    @Test
    fun `SearchModels intent filters models by query`() = runTest {
        viewModel.onIntent(RegistryIntent.SearchModels("llama"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isSearching)
        assertEquals("llama", state.searchQuery)
        assertEquals(1, state.models.size)
        assertEquals("llama3", state.models[0].name)
        assertNull(state.error)
    }

    @Test
    fun `SearchModels clears isSearching after the search completes`() = runTest {
        viewModel.onIntent(RegistryIntent.SearchModels("llama"))

        // With the unconfined dispatcher the search coroutine runs to
        // completion eagerly, so the transient isSearching=true state is not
        // observable; the contract is that it is false once the search lands.
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSearching)
        assertEquals(1, viewModel.state.value.models.size)
    }

    @Test
    fun `SearchModels intent returns empty results for non-matching query`() = runTest {
        viewModel.onIntent(RegistryIntent.SearchModels("nonexistent"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("nonexistent", state.searchQuery)
        assertTrue(state.models.isEmpty())
        assertFalse(state.canLoadMore) // No results means can't load more
        assertNull(state.error)
    }

    @Test
    fun `SearchModels intent handles search failure`() = runTest {
        val failingRepository = object : RegistryRepository {
            override suspend fun getModels(query: String, page: Int, sort: RegistrySort, category: String?): Result<List<RegistryModel>> {
                return Result.failure(Exception("Search failed"))
            }
        }

        val failingUseCase = GetRegistryModelsUseCase(failingRepository, testDispatcher)

        val failingViewModel = RegistryViewModel(failingUseCase, pullModelUseCase)

        failingViewModel.onIntent(RegistryIntent.SearchModels("test"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Search failed", state.error.cause?.message)
    }

    @Test
    fun `ClearSearch intent clears search and reloads all models`() = runTest {
        // First perform a search
        viewModel.onIntent(RegistryIntent.SearchModels("llama"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("llama", viewModel.state.value.searchQuery)

        // Clear search
        viewModel.onIntent(RegistryIntent.ClearSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("", state.searchQuery)
        assertEquals(testModels.size, state.models.size)
        assertNull(state.error)
    }

    @Test
    fun `Retry intent reloads models after error`() = runTest {
        // First trigger an error
        val failingRepository = object : RegistryRepository {
            private var attemptCount = 0
            override suspend fun getModels(query: String, page: Int, sort: RegistrySort, category: String?): Result<List<RegistryModel>> {
                return if (attemptCount++ == 0) {
                    Result.failure(Exception("First attempt failed"))
                } else {
                    Result.success(testModels)
                }
            }
        }

        val retryUseCase = GetRegistryModelsUseCase(failingRepository, testDispatcher)

        val retryViewModel = RegistryViewModel(retryUseCase, pullModelUseCase)

        // First attempt fails
        retryViewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(retryViewModel.state.value.error)

        // Retry succeeds
        retryViewModel.onIntent(RegistryIntent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = retryViewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(testModels.size, state.models.size)
    }

    @Test
    fun `LoadMore intent loads more models when available`() = runTest {
        // First load initial models
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.currentPage)

        // Try to load more (will return empty list from our mock)
        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingMore)
        assertEquals(2, state.currentPage)
        assertFalse(state.canLoadMore) // Empty response means no more models
    }

    @Test
    fun `LoadMore intent does nothing when already loading more`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        // Try to load multiple times rapidly - the second should be guarded
        viewModel.onIntent(RegistryIntent.LoadMore)
        viewModel.onIntent(RegistryIntent.LoadMore) // This should be guarded by isLoadingMore check
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only increment once due to guard condition
        val state = viewModel.state.value
        assertEquals(2, state.currentPage)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `LoadMore intent does nothing when canLoadMore is false`() = runTest {
        // First load models
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        // Load more to get empty response which sets canLoadMore to false
        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.canLoadMore)

        val currentPageBefore = viewModel.state.value.currentPage

        // Try to load more when can't load more
        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        // Current page should not change due to guard condition
        assertEquals(currentPageBefore, viewModel.state.value.currentPage)
    }

    @Test
    fun `LoadMore intent handles load failure`() = runTest {
        val failingRepository = object : RegistryRepository {
            private var callCount = 0
            override suspend fun getModels(query: String, page: Int, sort: RegistrySort, category: String?): Result<List<RegistryModel>> {
                return if (callCount++ == 0) {
                    Result.success(testModels)
                } else {
                    Result.failure(Exception("Load more failed"))
                }
            }
        }

        val failingUseCase = GetRegistryModelsUseCase(failingRepository, testDispatcher)

        val failingViewModel = RegistryViewModel(failingUseCase, pullModelUseCase)

        // Load initial models successfully
        failingViewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testModels.size, failingViewModel.state.value.models.size)

        // Try to load more and fail
        failingViewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        assertFalse(state.isLoadingMore)
        assertFalse(state.canLoadMore) // Failure sets canLoadMore to false
    }

    @Test
    fun `ShowPullDialog intent sets up pull dialog state`() = runTest {
        val model = testModels[0]
        viewModel.onIntent(RegistryIntent.ShowPullDialog(model))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(model.name, state.pullModelName)
        assertNull(state.pullResult)
        assertEquals("latest", state.selectedTag)

        // Should include "latest" even if not in original tags
        assertTrue(state.availableTags.contains("latest"))
        assertTrue(state.availableTags.contains("8b"))
        assertTrue(state.availableTags.contains("70b"))
    }

    @Test
    fun `ShowPullDialog intent adds latest tag if not present`() = runTest {
        val modelWithoutLatest = RegistryModel(
            id = "custom",
            name = "custom",
            description = "Custom model",
            pullCount = 100,
            tags = listOf("v1", "v2"), // No "latest" tag
            capabilities = listOf("chat"),
            updatedAt = "2024-01-01"
        )

        viewModel.onIntent(RegistryIntent.ShowPullDialog(modelWithoutLatest))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.availableTags.contains("latest"))
        assertTrue(state.availableTags.contains("v1"))
        assertTrue(state.availableTags.contains("v2"))
        assertEquals("latest", state.selectedTag)
    }

    @Test
    fun `SelectTag intent updates selected tag`() = runTest {
        // First show pull dialog
        viewModel.onIntent(RegistryIntent.ShowPullDialog(testModels[0]))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("latest", viewModel.state.value.selectedTag)

        // Select different tag
        viewModel.onIntent(RegistryIntent.SelectTag("8b"))

        val state = viewModel.state.value
        assertEquals("8b", state.selectedTag)
    }

    @Test
    fun `PullModel intent starts pull operation`() = runTest {
        // First show pull dialog
        viewModel.onIntent(RegistryIntent.ShowPullDialog(testModels[0]))
        testDispatcher.scheduler.advanceUntilIdle()

        // Start pull
        viewModel.onIntent(RegistryIntent.PullModel("llama3", "latest"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("llama3", state.pullModelName)
        assertNotNull(state.pullResult)
        assertTrue(state.pullResult is PullResult.Starting || state.pullResult is PullResult.Completed)
    }

    @Test
    fun `PullModel intent handles pull failure`() = runTest {
        val failingRepository = object : OllamaRepository {
            override suspend fun getModels(): Result<List<OllamaModel>> {
                return Result.success(emptyList())
            }

            override suspend fun getRunningModels(): Result<List<RunningModel>> {
                return Result.success(emptyList())
            }

            override suspend fun deleteModel(name: String): Result<Unit> {
                return Result.success(Unit)
            }

            override fun pullModel(modelName: String): Flow<Result<PullProgress>> {
                return flowOf(Result.failure(Exception("Pull operation failed")))
            }
        }

        val failingPullUseCase = PullModelUseCase(failingRepository)
        val failingViewModel = RegistryViewModel(getRegistryModelsUseCase, failingPullUseCase)

        // First show pull dialog
        failingViewModel.onIntent(RegistryIntent.ShowPullDialog(testModels[0]))
        testDispatcher.scheduler.advanceUntilIdle()

        // Start pull that will fail
        failingViewModel.onIntent(RegistryIntent.PullModel("llama3", "latest"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = failingViewModel.state.value
        // PullModelUseCase now surfaces failures as PullResult.Error
        assertNotNull(state.pullResult)
        assertTrue(state.pullResult is PullResult.Error)
    }

    @Test
    fun `ResetPullState intent clears pull state and cancels pull job`() = runTest {
        // Start a pull operation
        viewModel.onIntent(RegistryIntent.ShowPullDialog(testModels[0]))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.PullModel("llama3", "latest"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify pull state is set
        assertNotNull(viewModel.state.value.pullModelName)
        assertNotNull(viewModel.state.value.pullResult)

        // Reset pull state
        viewModel.onIntent(RegistryIntent.ResetPullState)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.pullModelName)
        assertNull(state.pullResult)
        assertNull(state.selectedTag)
        assertTrue(state.availableTags.isEmpty())
    }

    @Test
    fun `ShowPullDialog followed by PullModel completes full pull flow`() = runTest {
        // Show pull dialog
        viewModel.onIntent(RegistryIntent.ShowPullDialog(testModels[0]))
        testDispatcher.scheduler.advanceUntilIdle()

        var dialogState = viewModel.state.value
        assertEquals("llama3", dialogState.pullModelName)
        assertEquals("latest", dialogState.selectedTag)

        // Select specific tag
        viewModel.onIntent(RegistryIntent.SelectTag("8b"))
        testDispatcher.scheduler.advanceUntilIdle()

        dialogState = viewModel.state.value
        assertEquals("8b", dialogState.selectedTag)

        // Start pull with selected tag
        viewModel.onIntent(RegistryIntent.PullModel("llama3", "8b"))
        testDispatcher.scheduler.advanceUntilIdle()

        val pullState = viewModel.state.value
        assertEquals("llama3", pullState.pullModelName)
        assertNotNull(pullState.pullResult)

        // Reset after pull
        viewModel.onIntent(RegistryIntent.ResetPullState)
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.state.value
        assertNull(finalState.pullModelName)
        assertNull(finalState.pullResult)
        assertNull(finalState.selectedTag)
        assertTrue(finalState.availableTags.isEmpty())
    }

    @Test
    fun `multiple LoadMore calls properly increment page number`() = runTest {
        // Create a repository that returns models for multiple pages
        val multiPageRepository = object : RegistryRepository {
            override suspend fun getModels(query: String, page: Int, sort: RegistrySort, category: String?): Result<List<RegistryModel>> {
                return when (page) {
                    1 -> Result.success(testModels)
                    2 -> Result.success(testModels.take(1)) // Return one model on page 2
                    else -> Result.success(emptyList())
                }
            }
        }

        val multiPageUseCase = GetRegistryModelsUseCase(multiPageRepository, testDispatcher)

        val multiPageViewModel = RegistryViewModel(multiPageUseCase, pullModelUseCase)

        multiPageViewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, multiPageViewModel.state.value.currentPage)

        multiPageViewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, multiPageViewModel.state.value.currentPage)
    }

    @Test
    fun `SearchModels resets pagination state`() = runTest {
        // Load models and advance to page 2
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.currentPage)

        // Perform search - should reset to page 1
        viewModel.onIntent(RegistryIntent.SearchModels("test"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.currentPage)
        assertEquals("test", state.searchQuery)
    }

    @Test
    fun `SelectSort intent reloads from page one with the selected sort`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.SelectSort(RegistrySort.NEWEST))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(RegistrySort.NEWEST, state.sort)
        assertEquals(RegistrySort.NEWEST, requestedSort)
        assertEquals(1, state.currentPage)
        assertFalse(state.isLoading)
        assertEquals(testModels.size, state.models.size)
        assertNull(state.error)
    }

    @Test
    fun `SelectSort with the same sort does not fetch again`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fetchCount)

        viewModel.onIntent(RegistryIntent.SelectSort(RegistrySort.POPULAR))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fetchCount, "Re-selecting the active sort must not trigger a reload")
    }

    @Test
    fun `SelectCategory intent filters models by capability`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.SelectCategory("instruct"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("instruct", state.selectedCategory)
        assertEquals("instruct", requestedCategory)
        assertEquals(1, state.models.size)
        assertEquals("llama3", state.models[0].name)
        assertEquals(1, state.currentPage)
        assertNull(state.error)
    }

    @Test
    fun `SelectCategory with null clears the filter`() = runTest {
        viewModel.onIntent(RegistryIntent.SelectCategory("instruct"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.models.size)

        viewModel.onIntent(RegistryIntent.SelectCategory(null))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.selectedCategory)
        assertNull(requestedCategory)
        assertEquals(testModels.size, state.models.size)
    }

    @Test
    fun `SelectSort resets pagination to page one`() = runTest {
        viewModel.onIntent(RegistryIntent.LoadModels)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.currentPage)

        viewModel.onIntent(RegistryIntent.SelectSort(RegistrySort.NEWEST))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.currentPage)
        assertEquals(RegistrySort.NEWEST, requestedSort)
    }

    @Test
    fun `search preserves the active category filter`() = runTest {
        viewModel.onIntent(RegistryIntent.SelectCategory("chat"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.SearchModels("mistral"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("chat", state.selectedCategory)
        assertEquals("mistral", state.searchQuery)
        assertEquals(1, state.models.size)
        assertEquals("mistral", state.models[0].name)
    }

    @Test
    fun `LoadMore keeps the active sort and category`() = runTest {
        viewModel.onIntent(RegistryIntent.SelectSort(RegistrySort.NEWEST))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(RegistryIntent.SelectCategory("chat"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(RegistryIntent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.currentPage)
        assertEquals(RegistrySort.NEWEST, requestedSort)
        assertEquals("chat", requestedCategory)
    }
}