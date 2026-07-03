package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.presentation.components.EmptyView
import by.ciszkin.herdmanager.presentation.components.HerdTopBar
import by.ciszkin.herdmanager.presentation.components.ErrorView
import by.ciszkin.herdmanager.presentation.error.toUserMessageString
import by.ciszkin.herdmanager.presentation.components.LoadingView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.delete_failed
import herdmanager.shared.generated.resources.empty_models
import herdmanager.shared.generated.resources.local_models
import herdmanager.shared.generated.resources.model_deleted
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
object ModelListScreen : Screen {
    private fun readResolve(): Any = ModelListScreen

    @Composable
    override fun Content() {
        val getModelsUseCase = koinInject<by.ciszkin.herdmanager.domain.usecase.GetModelsUseCase>()
        val deleteModelUseCase = koinInject<by.ciszkin.herdmanager.domain.usecase.DeleteModelUseCase>()
        val viewModel = rememberScreenModel {
            ModelListViewModel(
                getModelsUseCase = getModelsUseCase,
                deleteModelUseCase = deleteModelUseCase
            )
        }
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showDeleteDialog by remember { mutableStateOf<String?>(null) }
        var showDetailsDialog by remember { mutableStateOf<OllamaModel?>(null) }
        val modelDeletionSuccessMessage = stringResource(Res.string.model_deleted)
        val modelDeletionFailureMessage = stringResource(Res.string.delete_failed)

        LaunchedEffect(Unit) {
            viewModel.onIntent(ModelListIntent.Refresh)
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ModelListEffect.ShowToast ->
                        snackbarHostState.showSnackbar(effect.error.toUserMessageString())
                    is ModelListEffect.ShowModelDeletionSuccess ->
                        snackbarHostState.showSnackbar(modelDeletionSuccessMessage)
                    is ModelListEffect.ShowModelDeletionFailure ->
                        snackbarHostState.showSnackbar(modelDeletionFailureMessage)
                    is ModelListEffect.ShowDeleteConfirmation ->
                        showDeleteDialog = effect.modelName
                }
            }
        }

        Scaffold(
            topBar = {
                HerdTopBar(
                    title = stringResource(Res.string.local_models),
                    onRefresh = { viewModel.onIntent(ModelListIntent.Refresh) },
                    isLoading = state.isLoading
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when {
                state.isLoading && state.models.isEmpty() -> LoadingView()
                state.error != null -> ErrorView(
                    error = state.error,
                    onRetry = { viewModel.onIntent(ModelListIntent.Retry) }
                )
                !state.isLoading && state.models.isEmpty() -> EmptyView(
                    stringResource(Res.string.empty_models)
                )
                else -> ModelGrid(
                    models = state.models,
                    onDelete = { viewModel.onIntent(ModelListIntent.DeleteModel(it)) },
                    onShowDetails = { showDetailsDialog = it },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        showDeleteDialog?.let { modelName ->
            DeleteConfirmationDialog(
                modelName = modelName,
                onConfirm = {
                    viewModel.onIntent(ModelListIntent.ConfirmDelete(modelName))
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }

        showDetailsDialog?.let { model ->
            ModelDetailsDialog(
                model = model,
                onDismiss = { showDetailsDialog = null }
            )
        }
    }
}
