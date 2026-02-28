package by.ciszkin.herdmanager.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import by.ciszkin.herdmanager.di.AppModule
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.X

@OptIn(ExperimentalMaterial3Api::class)
object SettingsScreen : Screen {
    private fun readResolve(): Any = SettingsScreen

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel {
            SettingsViewModel(
                observeSettingsUseCase = AppModule.observeSettingsUseCase,
                saveSettingsUseCase = AppModule.saveSettingsUseCase
            )
        }
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.onIntent(SettingsIntent.LoadSettings)
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                    SettingsEffect.SettingsSaved -> {
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    actions = {
                        IconButton(onClick = { viewModel.onIntent(SettingsIntent.ResetToDefaults) }) {
                            Icon(FeatherIcons.RotateCcw, "Reset to defaults")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Server Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                state.settings?.let { settings ->
                    SettingsTextField(
                        label = "Server URL",
                        value = settings.serverUrl,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateServerUrl(it)) },
                        placeholder = "http://localhost:11434",
                        keyboardType = KeyboardType.Uri
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Polling Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SettingsSlider(
                        label = "Refresh Interval (seconds)",
                        value = settings.refreshInterval,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateRefreshInterval(it)) },
                        valueRange = 1f..60f,
                        valueLabel = "${settings.refreshInterval} seconds"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Polling",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = settings.pollingEnabled,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdatePollingEnabled(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isDirty) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.SaveSettings(settings))
                                },
                                enabled = !state.isSaving,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(24.dp)
                                    )
                                } else {
                                    Icon(FeatherIcons.Check, "Save", Modifier.padding(end = 8.dp))
                                    Text("Save Settings")
                                }
                            }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                viewModel.onIntent(SettingsIntent.DiscardChanges)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSaving
                        ) {
                            Icon(FeatherIcons.X, "Reset", Modifier.padding(end = 8.dp))
                            Text("Discard")
                        }
                        }
                    }
                }

                state.saveError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
