package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.presentation.components.EmptyView
import by.ciszkin.herdmanager.presentation.components.ErrorView
import by.ciszkin.herdmanager.presentation.components.LoadingView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.delete_failed
import herdmanager.composeapp.generated.resources.empty_models
import herdmanager.composeapp.generated.resources.local_models
import herdmanager.composeapp.generated.resources.model_deleted
import herdmanager.composeapp.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
object ModelListScreen : Screen {
    private fun readResolve(): Any = ModelListScreen

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel {
            ModelListViewModel(
                getModelsUseCase = AppModule.getModelsUseCase,
                deleteModelUseCase = AppModule.deleteModelUseCase
            )
        }
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
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
                    is ModelListEffect.ShowToast -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                    is ModelListEffect.ShowDeleteConfirmation -> {
                        showDeleteDialog = effect.modelName
                    }
                    is ModelListEffect.ShowModelDeletionSuccess -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(modelDeletionSuccessMessage)
                        }
                    }
                    is ModelListEffect.ShowModelDeletionFailure -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(modelDeletionFailureMessage)
                        }
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.local_models)) },
                    actions = {
                        IconButton(onClick = { viewModel.onIntent(ModelListIntent.Refresh) }) {
                            Icon(FeatherIcons.RefreshCw, stringResource(Res.string.refresh))
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when {
                state.isLoading && state.models.isEmpty() -> LoadingView()
                !state.isLoading && state.models.isEmpty() -> EmptyView(
                    stringResource(Res.string.empty_models)
                )
                state.error != null -> ErrorView(
                    error = state.error,
                    onRetry = { viewModel.onIntent(ModelListIntent.Retry) }
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
