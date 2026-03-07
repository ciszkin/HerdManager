package by.ciszkin.herdmanager.presentation.registry

import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.usecase.GetRegistryModelsUseCase
import by.ciszkin.herdmanager.domain.usecase.PullModelUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RegistryViewModel(
    private val getRegistryModelsUseCase: GetRegistryModelsUseCase,
    private val pullModelUseCase: PullModelUseCase
) : BaseMviViewModel<RegistryIntent, RegistryState, RegistryEffect>() {

    private var pullJob: Job? = null

    override fun initialState() = RegistryState()

    override fun onIntent(intent: RegistryIntent) {
        when (intent) {
            RegistryIntent.LoadModels -> loadModels()
            is RegistryIntent.SearchModels -> filterModels(intent.query)
            RegistryIntent.ClearSearch -> filterModels()
            RegistryIntent.Retry -> loadModels()
            RegistryIntent.LoadMore -> loadMore()
            is RegistryIntent.ShowPullDialog -> showPullDialog(intent.model)
            is RegistryIntent.SelectTag -> selectTag(intent.tag)
            is RegistryIntent.PullModel -> startPull(intent.modelName, intent.tag)
            RegistryIntent.ResetPullState -> resetPullState()
        }
    }

    private fun loadModels() {
        screenModelScope.launch {
            reduceState { copy(isLoading = true, error = null, currentPage = 1, canLoadMore = true) }
            delay(100)
            getRegistryModelsUseCase(page = 1)
                .onSuccess { models ->
                    reduceState { copy(models = models, allModels = models, isLoading = false, canLoadMore = models.isNotEmpty()) }
                    sendEffect(RegistryEffect.ScrollToTop)
                }
                .onFailure { error ->
                    reduceState { copy(error = error.message, isLoading = false) }
                }
        }
    }

    private fun filterModels(query: String = "") {
        screenModelScope.launch {
            reduceState { copy(isLoading = true, searchQuery = query, currentPage = 1, canLoadMore = true, allModels = emptyList()) }
            delay(100)
            getRegistryModelsUseCase(query, page = 1)
                .onSuccess { models ->
                    reduceState { copy(models = models, allModels = emptyList(), isLoading = false, canLoadMore = models.isNotEmpty()) }
                }
                .onFailure { error ->
                    reduceState { copy(error = error.message, isLoading = false) }
                }
        }
    }

    private fun loadMore() {
        val currentState = state.value
        if (currentState.isLoadingMore || !currentState.canLoadMore) return

        screenModelScope.launch {
            val nextPage = currentState.currentPage + 1
            reduceState { copy(isLoadingMore = true) }
            delay(100)
            getRegistryModelsUseCase(currentState.searchQuery, nextPage)
                .onSuccess { newModels ->
                    val existingIds = currentState.models.map { it.id }.toSet()
                    val uniqueNewModels = newModels.filter { it.id !in existingIds }
                    val allModels = currentState.models + uniqueNewModels
                    reduceState { 
                        copy(
                            models = allModels, 
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
            val tags = if ("latest" in model.tags) {
                model.tags
            } else {
                model.tags + "latest"
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
                    reduceState {
                        copy(
                            pullResult = PullResult.Error(
                                error.message ?: "Pull failed"
                            )
                        )
                    }
                    sendEffect(RegistryEffect.ShowToast(error.message ?: "Pull failed"))
                }
                .collect { result ->
                    if (result is PullResult.Error) {
                        sendEffect(RegistryEffect.ShowToast(result.message))
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
