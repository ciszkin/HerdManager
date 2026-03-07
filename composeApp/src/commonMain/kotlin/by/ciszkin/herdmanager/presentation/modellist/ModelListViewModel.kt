package by.ciszkin.herdmanager.presentation.modellist

import by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase
import by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase
import by.ciszkin.herdmanager.presentation.architecture.BaseMviViewModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

class ModelListViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val deleteModelUseCase: DeleteModelUseCase
) : BaseMviViewModel<ModelListIntent, ModelListState, ModelListEffect>() {

    override fun initialState() = ModelListState()

    override fun onIntent(intent: ModelListIntent) {
        when (intent) {
            ModelListIntent.Refresh -> loadModels()
            is ModelListIntent.DeleteModel -> confirmDelete(intent.modelName)
            is ModelListIntent.ConfirmDelete -> deleteModel(intent.modelName)
            ModelListIntent.Retry -> loadModels()
        }
    }

    private fun loadModels() {
        screenModelScope.launch {
            reduceState { copy(isLoading = true, error = null) }
            getModelsUseCase()
                .onSuccess { models -> reduceState { copy(models = models, isLoading = false) } }
                .onFailure { error -> reduceState { copy(error = error.message, isLoading = false) } }
        }
    }

    private fun confirmDelete(modelName: String) {
        sendEffect(ModelListEffect.ShowDeleteConfirmation(modelName))
    }

    private fun deleteModel(modelName: String) {
        screenModelScope.launch {
            reduceState { copy(isDeleting = true, modelToDelete = modelName) }
            deleteModelUseCase(modelName)
                .onSuccess {
                    loadModels()
                    sendEffect(ModelListEffect.ShowModelDeletionSuccess)
                }
                .onFailure {
                    sendEffect(ModelListEffect.ShowModelDeletionFailure)
                }
            reduceState { copy(isDeleting = false, modelToDelete = null) }
        }
    }
}
