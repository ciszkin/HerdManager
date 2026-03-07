package by.ciszkin.herdmanager.presentation.modellist

import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.presentation.architecture.MviEffect
import by.ciszkin.herdmanager.presentation.architecture.MviIntent
import by.ciszkin.herdmanager.presentation.architecture.MviState

sealed interface ModelListIntent : MviIntent {
    data object Refresh : ModelListIntent
    data class DeleteModel(val modelName: String) : ModelListIntent
    data class ConfirmDelete(val modelName: String) : ModelListIntent
    data object Retry : ModelListIntent
}

data class ModelListState(
    val models: List<OllamaModel> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val modelToDelete: String? = null,
    val error: String? = null
) : MviState

sealed interface ModelListEffect : MviEffect {
    data object ShowModelDeletionSuccess : ModelListEffect
    data object ShowModelDeletionFailure : ModelListEffect
    data class ShowToast(val message: String) : ModelListEffect
    data class ShowDeleteConfirmation(val modelName: String) : ModelListEffect
}
