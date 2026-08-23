package by.ciszkin.herdmanager.presentation.registry

import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import by.ciszkin.herdmanager.presentation.architecture.MviEffect
import by.ciszkin.herdmanager.presentation.architecture.MviIntent
import by.ciszkin.herdmanager.presentation.architecture.MviState

/**
 * Quick-filter capabilities offered on the registry screen, matching the
 * capability `c=` values verified to be honored by ollama.com/search.
 */
val REGISTRY_FILTER_CATEGORIES = listOf("vision", "embedding", "tools", "thinking")

sealed interface RegistryIntent : MviIntent {
    data object LoadModels : RegistryIntent
    data class SearchModels(val query: String) : RegistryIntent
    data object ClearSearch : RegistryIntent
    data object Retry : RegistryIntent
    data object LoadMore : RegistryIntent
    data class SelectSort(val sort: RegistrySort) : RegistryIntent
    data class SelectCategory(val category: String?) : RegistryIntent
    data class ShowPullDialog(val model: RegistryModel) : RegistryIntent
    data class SelectTag(val tag: String) : RegistryIntent
    data class PullModel(val modelName: String, val tag: String) : RegistryIntent
    data object ResetPullState : RegistryIntent
}

data class RegistryState(
    val models: List<RegistryModel> = emptyList(),
    val searchQuery: String = "",
    val sort: RegistrySort = RegistrySort.POPULAR,
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val currentPage: Int = 1,
    val error: AppError? = null,
    val pullModelName: String? = null,
    val pullResult: PullResult? = null,
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList()
) : MviState

sealed interface RegistryEffect : MviEffect {
    data class ShowToast(val error: AppError) : RegistryEffect
    data object ScrollToTop : RegistryEffect
}
