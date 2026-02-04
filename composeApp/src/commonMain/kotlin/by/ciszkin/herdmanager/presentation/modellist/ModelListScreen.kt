package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.OllamaModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw

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
        var showDeleteDialog by remember { mutableStateOf<String?>(null) }
        var showDetailsDialog by remember { mutableStateOf<OllamaModel?>(null) }

        LaunchedEffect(Unit) {
            viewModel.onIntent(ModelListIntent.Refresh)
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ModelListEffect.ShowToast -> {
                    }
                    is ModelListEffect.ShowDeleteConfirmation -> {
                        showDeleteDialog = effect.modelName
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Local Models") },
                    actions = {
                        IconButton(onClick = { viewModel.onIntent(ModelListIntent.Refresh) }) {
                            Icon(FeatherIcons.RefreshCw, "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                state.isLoading && state.models.isEmpty() -> LoadingView()
                state.error != null && state.models.isEmpty() -> ErrorView(
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
                onConfirm = { viewModel.onIntent(ModelListIntent.ConfirmDelete(modelName)) },
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

@Composable
fun ModelGrid(
    models: List<OllamaModel>,
    onDelete: (String) -> Unit,
    onShowDetails: (OllamaModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(models, key = { it.name }) { model ->
            ModelCard(
                model = model,
                onDelete = onDelete,
                onShowDetails = onShowDetails
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error: $error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
