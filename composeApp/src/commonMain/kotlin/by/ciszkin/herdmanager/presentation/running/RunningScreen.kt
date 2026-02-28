package by.ciszkin.herdmanager.presentation.running

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.presentation.components.EmptyView
import by.ciszkin.herdmanager.presentation.components.ErrorView
import by.ciszkin.herdmanager.presentation.components.LoadingView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw

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
        val rotation = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            viewModel.onIntent(RunningIntent.Initialize)
            viewModel.effect.collect { effect ->
                when (effect) {
                    RunningEffect.AnimateRefreshIcon -> {
                        rotation.apply {
                            snapTo(0f)
                            animateTo(
                                targetValue = 360f,
                                animationSpec = tween(durationMillis = 1000),
                            )
                        }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.onIntent(RunningIntent.StopPolling)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Running Models") },
                    actions = {
                        IconButton(onClick = {
                            viewModel.onIntent(RunningIntent.Refresh)
                        }) {
                            Icon(
                                imageVector = FeatherIcons.RefreshCw,
                                contentDescription = "Refresh",
                                tint = if (state.pollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.rotate(rotation.value)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            when {
                state.isLoading && state.models.isEmpty() -> LoadingView()
                !state.isLoading && state.models.isEmpty() -> EmptyView()
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
