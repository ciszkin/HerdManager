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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.ThemeMode
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.X
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.discard
import herdmanager.composeapp.generated.resources.language
import herdmanager.composeapp.generated.resources.polling_enabled
import herdmanager.composeapp.generated.resources.polling_settings
import herdmanager.composeapp.generated.resources.refresh_interval
import herdmanager.composeapp.generated.resources.refresh_interval_seconds
import herdmanager.composeapp.generated.resources.reset_to_defaults
import herdmanager.composeapp.generated.resources.save_settings
import herdmanager.composeapp.generated.resources.server_config
import herdmanager.composeapp.generated.resources.server_url
import herdmanager.composeapp.generated.resources.server_url_placeholder
import herdmanager.composeapp.generated.resources.settings
import herdmanager.composeapp.generated.resources.settings_saved
import herdmanager.composeapp.generated.resources.theme
import org.jetbrains.compose.resources.stringResource

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
        val settingsSavedMessage = stringResource(Res.string.settings_saved)

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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(settingsSavedMessage)
                        }
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings)) },
                    actions = {
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
                Text(
                    text = stringResource(Res.string.server_config),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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

                    SettingsSlider(
                        label = stringResource(Res.string.refresh_interval),
                        value = settings.refreshInterval,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateRefreshInterval(it)) },
                        valueRange = 1f..60f,
                        valueLabel = stringResource(Res.string.refresh_interval_seconds).format(settings.refreshInterval)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.language),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        val fillMaxWidth = Modifier
                            .fillMaxWidth()
                        OutlinedTextField(
                            value = AvailableLanguage.fromCode(settings.language).getLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.language)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = fillMaxWidth.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Column {
                                AvailableLanguage.entries.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language.getLabel()) },
                                        onClick = {
                                            viewModel.onIntent(SettingsIntent.UpdateLanguage(language.code))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    var themeExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = settings.themeMode.getLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.theme)) },
                            leadingIcon = { Icon(settings.themeMode.icon, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        )

                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            Column {
                                ThemeMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(mode.icon, contentDescription = null) },
                                        text = { Text(mode.getLabel()) },
                                        onClick = {
                                            viewModel.onIntent(SettingsIntent.UpdateThemeMode(mode))
                                            themeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
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
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
