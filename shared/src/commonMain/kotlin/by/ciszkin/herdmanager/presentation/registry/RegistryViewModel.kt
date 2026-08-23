package by.ciszkin.herdmanager.presentation.registry

import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import by.ciszkin.herdmanager.domain.error.mapper.toAppError
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RegistryViewModel(
    private val getRegistryModelsUseCase: GetRegistryModelsUseCase,
    private val pullModelUseCase: PullModelUseCase
) : BaseMviViewModel<RegistryIntent, RegistryState, RegistryEffect>() {

    private var pullJob: Job? = null

    companion object {
        /** Ollama tag constraint: one or more [a-zA-Z0-9._-] characters. */
        private val VALID_TAG = Regex("""[a-zA-Z0-9._-]+""")
    }

    override fun initialState() = RegistryState()

    override fun onIntent(intent: RegistryIntent) {
        when (intent) {
            RegistryIntent.LoadModels -> loadModels()
            is RegistryIntent.SearchModels -> filterModels(intent.query)
            RegistryIntent.ClearSearch -> filterModels()
            RegistryIntent.Retry -> loadModels()
            RegistryIntent.LoadMore -> loadMore()
            is RegistryIntent.SelectSort -> applySort(intent.sort)
            is RegistryIntent.SelectCategory -> applyCategory(intent.category)
            is RegistryIntent.ShowPullDialog -> showPullDialog(intent.model)
            is RegistryIntent.SelectTag -> selectTag(intent.tag)
            is RegistryIntent.PullModel -> startPull(intent.modelName, intent.tag)
            RegistryIntent.ResetPullState -> resetPullState()
        }
    }

    private fun loadModels() {
        screenModelScope.launch {
            reduceState { copy(isLoading = true, error = null, currentPage = 1, canLoadMore = true) }
            fetchFirstPage()
        }
    }

    /**
     * Loads page 1 applying the current search query, sort order and category
     * filter. Reads them from the state, so callers only need to reduce the
     * option they are changing before invoking it.
     */
    private suspend fun fetchFirstPage() {
        val current = state.value
        getRegistryModelsUseCase(
            query = current.searchQuery,
            page = 1,
            sort = current.sort,
            category = current.selectedCategory
        )
            .onSuccess { models ->
                reduceState {
                    copy(models = models, isLoading = false, isSearching = false, canLoadMore = models.isNotEmpty())
                }
                sendEffect(RegistryEffect.ScrollToTop)
            }
            .onFailure { error ->
                reduceState { copy(error = error.toAppError(), isLoading = false, isSearching = false) }
            }
    }

    private fun filterModels(query: String = "") {
        screenModelScope.launch {
            reduceState {
                copy(
                    isLoading = true,
                    isSearching = true,
                    searchQuery = query,
                    currentPage = 1,
                    canLoadMore = true
                )
            }
            fetchFirstPage()
        }
    }

    private fun applySort(sort: RegistrySort) {
        if (state.value.sort == sort) return
        screenModelScope.launch {
            reduceState {
                copy(sort = sort, isLoading = true, error = null, currentPage = 1, canLoadMore = true)
            }
            fetchFirstPage()
        }
    }

    private fun applyCategory(category: String?) {
        if (state.value.selectedCategory == category) return
        screenModelScope.launch {
            reduceState {
                copy(
                    selectedCategory = category,
                    isLoading = true,
                    error = null,
                    currentPage = 1,
                    canLoadMore = true
                )
            }
            fetchFirstPage()
        }
    }

    private fun loadMore() {
        val currentState = state.value
        if (currentState.isLoadingMore || !currentState.canLoadMore) return

        screenModelScope.launch {
            val nextPage = currentState.currentPage + 1
            reduceState { copy(isLoadingMore = true) }
            getRegistryModelsUseCase(
                query = currentState.searchQuery,
                page = nextPage,
                sort = currentState.sort,
                category = currentState.selectedCategory
            )
                .onSuccess { newModels ->
                    val existingIds = currentState.models.map { it.id }.toSet()
                    val uniqueNewModels = newModels.filter { it.id !in existingIds }
                    val mergedModels = currentState.models + uniqueNewModels
                    reduceState {
                        copy(
                            models = mergedModels,
                            isLoadingMore = false,
                            currentPage = nextPage,
                            canLoadMore = newModels.isNotEmpty()
                        )
                    }
                }
                .onFailure {
                    reduceState { copy(isLoadingMore = false, canLoadMore = false) }
                }
        }
    }

    private fun showPullDialog(model: RegistryModel) {
        screenModelScope.launch {
            // The scraper's size tags are usually valid pull tags, but guard
            // against anything outside Ollama's tag charset ("model:tag").
            val safeTags = model.tags.filter { VALID_TAG.matches(it) }
            val tags = if ("latest" in safeTags) {
                safeTags
            } else {
                safeTags + "latest"
            }
            reduceState {
                copy(
                    pullModelName = model.name,
                    pullResult = null,
                    selectedTag = "latest",
                    availableTags = tags
                )
            }
        }
    }

    private fun selectTag(tag: String) {
        reduceState { copy(selectedTag = tag) }
    }

    private fun startPull(modelName: String, tag: String) {
        pullJob?.cancel()
        pullJob = screenModelScope.launch {
            reduceState {
                copy(
                    pullModelName = modelName,
                    pullResult = PullResult.Starting
                )
            }
            pullModelUseCase("$modelName:$tag")
                .catch { error ->
                    val appError = error.toAppError()
                    reduceState {
                        copy(
                            pullResult = PullResult.Error(appError)
                        )
                    }
                    sendEffect(RegistryEffect.ShowToast(appError))
                }
                .collect { result ->
                    if (result is PullResult.Error) {
                        sendEffect(RegistryEffect.ShowToast(result.error))
                    }
                    reduceState { copy(pullResult = result) }
                }
        }
    }

    private fun resetPullState() {
        pullJob?.cancel()
        pullJob = null
        reduceState {
            copy(
                pullModelName = null,
                pullResult = null,
                selectedTag = null,
                availableTags = emptyList()
            )
        }
    }
}
