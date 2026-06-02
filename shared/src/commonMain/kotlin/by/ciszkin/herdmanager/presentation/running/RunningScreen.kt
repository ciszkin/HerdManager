package by.ciszkin.herdmanager.presentation.running

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.presentation.components.EmptyView
import by.ciszkin.herdmanager.presentation.components.ErrorView
import by.ciszkin.herdmanager.presentation.components.HerdTopBar
import by.ciszkin.herdmanager.presentation.components.LoadingView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.empty_running
import herdmanager.shared.generated.resources.running_models
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
object RunningScreen : Screen {
    private fun readResolve(): Any = RunningScreen

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel {
            RunningViewModel(
                getRunningModelsUseCase = AppModule.getRunningModelsUseCase,
                observeSettingsUseCase = AppModule.observeSettingsUseCase
            )
        }
        val state by viewModel.state.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.onIntent(RunningIntent.Initialize)
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.onIntent(RunningIntent.StopPolling)
            }
        }

        Scaffold(
            topBar = {
                HerdTopBar(
                    title = stringResource(Res.string.running_models),
                    onRefresh = { viewModel.onIntent(RunningIntent.Refresh) },
                    buttonColor = if (state.pollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    isLoading = state.isLoading
                )
            }
        ) { padding ->
            when {
                state.isLoading && state.models.isEmpty() -> LoadingView()
                !state.isLoading && state.models.isEmpty() -> EmptyView(
                    stringResource(Res.string.empty_running)
                )
                state.error != null -> ErrorView(
                    error = state.error,
                    onRetry = { viewModel.onIntent(RunningIntent.Refresh) }
                )
                else -> RunningModelsList(
                    models = state.models,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
