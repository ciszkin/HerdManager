package by.ciszkin.herdmanager.presentation.registry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.PullResult
import by.ciszkin.herdmanager.presentation.components.EmptyView
import by.ciszkin.herdmanager.presentation.components.ErrorView
import by.ciszkin.herdmanager.presentation.components.LoadingView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
object RegistryScreen : Screen {
    private fun readResolve(): Any = RegistryScreen

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel {
            RegistryViewModel(
                getRegistryModelsUseCase = AppModule.getRegistryModelsUseCase,
                pullModelUseCase = AppModule.pullModelUseCase
            )
        }
        val state by viewModel.state.collectAsState()
        val gridState = rememberLazyGridState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        var showPullDialog by remember { mutableStateOf<Boolean>(false) }

        LaunchedEffect(gridState) {
            snapshotFlow {
                val layoutInfo = gridState.layoutInfo
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }
                .collect { lastVisible ->
                    val totalItems = gridState.layoutInfo.totalItemsCount
                    if (lastVisible >= totalItems - 6 && !state.isLoadingMore && state.canLoadMore && state.models.isNotEmpty()) {
                        viewModel.onIntent(RegistryIntent.LoadMore)
                    }
                }
        }

        LaunchedEffect(Unit) {
            viewModel.onIntent(RegistryIntent.LoadModels)
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is RegistryEffect.ShowToast -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }

                    is RegistryEffect.ScrollToTop -> {
                        launch {
                            gridState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }

        LaunchedEffect(state.pullResult) {
            if (state.pullResult is PullResult.Error) {
                showPullDialog = false
                viewModel.onIntent(RegistryIntent.ResetPullState)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Model Registry") },
                    actions = {
                        IconButton(onClick = { viewModel.onIntent(RegistryIntent.Retry) }) {
                            Icon(FeatherIcons.RefreshCw, "Refresh")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onIntent(RegistryIntent.SearchModels(it)) },
                    onClear = { viewModel.onIntent(RegistryIntent.ClearSearch) },
                    isLoading = state.isSearching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                when {
                    state.isLoading -> LoadingView()
                    !state.isLoading && state.models.isEmpty() -> EmptyView()
                    state.error != null -> ErrorView(
                        error = state.error,
                        onRetry = { viewModel.onIntent(RegistryIntent.Retry) }
                    )

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                            ) {
                                items(
                                    count = state.models.size,
                                    key = { index -> state.models[index].id }
                                ) { index ->
                                    val model = state.models[index]
                                    RegistryCard(
                                        model = model,
                                        onPull = {
                                            showPullDialog = true
                                            viewModel.onIntent(RegistryIntent.ShowPullDialog(model))
                                        }
                                    )
                                }
                                if (state.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPullDialog) {
            state.pullModelName?.let { pullModelName ->
                PullModelDialog(
                    modelName = pullModelName,
                    selectedTag = state.selectedTag,
                    availableTags = state.availableTags,
                    pullResult = state.pullResult,
                    onTagSelected = { viewModel.onIntent(RegistryIntent.SelectTag(it)) },
                    onPull = { viewModel.onIntent(RegistryIntent.PullModel(pullModelName, state.selectedTag ?: "")) },
                    onDismiss = {
                        showPullDialog = false
                        viewModel.onIntent(RegistryIntent.ResetPullState)
                    }
                )
            }
        }
    }
}
