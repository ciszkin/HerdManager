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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import by.ciszkin.herdmanager.presentation.components.HerdTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.presentation.components.UpdateBanner
import by.ciszkin.herdmanager.presentation.error.toUserMessage
import by.ciszkin.herdmanager.presentation.error.toUserMessageString
import by.ciszkin.herdmanager.util.openUrl
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Globe
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.X
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.appearance
import herdmanager.shared.generated.resources.discard
import herdmanager.shared.generated.resources.language
import herdmanager.shared.generated.resources.ollama_version
import herdmanager.shared.generated.resources.polling_enabled
import herdmanager.shared.generated.resources.polling_settings
import herdmanager.shared.generated.resources.refresh_interval
import herdmanager.shared.generated.resources.refresh_interval_seconds
import herdmanager.shared.generated.resources.reset_to_defaults
import herdmanager.shared.generated.resources.save_settings
import herdmanager.shared.generated.resources.server_config
import herdmanager.shared.generated.resources.server_url
import herdmanager.shared.generated.resources.server_url_placeholder
import herdmanager.shared.generated.resources.settings
import herdmanager.shared.generated.resources.settings_saved
import herdmanager.shared.generated.resources.theme
import org.jetbrains.compose.resources.stringResource

object SettingsScreen : Screen {
    private fun readResolve(): Any = SettingsScreen

    @Composable
    override fun Content() {
        val observeSettingsUseCase = koinInject<by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase>()
        val saveSettingsUseCase = koinInject<by.ciszkin.herdmanager.domain.usecase.SaveSettingsUseCase>()
        val checkForOllamaUpdateUseCase = koinInject<by.ciszkin.herdmanager.domain.usecase.CheckForOllamaUpdateUseCase>()
        val viewModel = rememberScreenModel {
            SettingsViewModel(
                observeSettingsUseCase = observeSettingsUseCase,
                saveSettingsUseCase = saveSettingsUseCase,
                checkForOllamaUpdateUseCase = checkForOllamaUpdateUseCase
            )
        }
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val settingsSavedMessage = stringResource(Res.string.settings_saved)

        LaunchedEffect(Unit) {
            viewModel.onIntent(SettingsIntent.LoadSettings)
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SettingsEffect.ShowToast ->
                        snackbarHostState.showSnackbar(effect.error.toUserMessageString())
                    is SettingsEffect.SettingsSaved ->
                        snackbarHostState.showSnackbar(settingsSavedMessage)
                }
            }
        }

        Scaffold(
            topBar = {
                HerdTopBar(
                    title = stringResource(Res.string.settings),
                    additionalActions = {
                        IconButton(onClick = { viewModel.onIntent(SettingsIntent.ResetToDefaults) }) {
                            Icon(FeatherIcons.RotateCcw, stringResource(Res.string.reset_to_defaults))
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
                // Show update banner if update is available
                val currentVersion = state.currentVersion
                val latestVersion = state.latestVersion
                if (state.isNewVersionAvailable && currentVersion != null && latestVersion != null) {
                    UpdateBanner(
                        latestVersion = latestVersion,
                        onOpenRelease = {
                            state.releaseUrl?.let { url -> openUrl(url) }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.server_config),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                currentVersion?.let { version ->
                    SettingsTextField(
                        label = stringResource(Res.string.ollama_version),
                        value = version,
                        onValueChange = {},
                        readOnly = true
                    )
                }

                state.settings?.let { settings ->
                    SettingsTextField(
                        label = stringResource(Res.string.server_url),
                        value = settings.serverUrl,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateServerUrl(it)) },
                        placeholder = stringResource(Res.string.server_url_placeholder),
                        keyboardType = KeyboardType.Uri
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.polling_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.polling_enabled),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = settings.pollingEnabled,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdatePollingEnabled(it)) }
                        )
                    }

                    if (settings.pollingEnabled) {
                        SettingsSlider(
                            label = stringResource(Res.string.refresh_interval),
                            value = settings.refreshInterval,
                            onValueChange = { viewModel.onIntent(SettingsIntent.UpdateRefreshInterval(it)) },
                            valueRange = 1f..60f,
                            valueLabel = stringResource(Res.string.refresh_interval_seconds).format(settings.refreshInterval)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.appearance),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SettingsDropdown(
                        label = Res.string.language,
                        items = AvailableLanguage.entries.toTypedArray(),
                        selectedItem = AvailableLanguage.fromCode(settings.language),
                        onItemSelected = { viewModel.onIntent(SettingsIntent.UpdateLanguage(it.code)) },
                        itemLabel = { it.getLabel() },
                        leadingIcon = FeatherIcons.Globe
                    )

                    SettingsDropdown(
                        label = Res.string.theme,
                        items = ThemeMode.entries.toTypedArray(),
                        selectedItem = settings.themeMode,
                        onItemSelected = { viewModel.onIntent(SettingsIntent.UpdateThemeMode(it)) },
                        itemLabel = { it.getLabel() },
                        leadingIcon = settings.themeMode.icon,
                        itemIcon = { it.icon }
                    )

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
                                    Text(stringResource(Res.string.save_settings))
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.DiscardChanges)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSaving
                            ) {
                                Icon(FeatherIcons.X, "Reset", Modifier.padding(end = 8.dp))
                                Text(stringResource(Res.string.discard))
                            }
                        }
                    }
                }

                state.saveError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error.toUserMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
