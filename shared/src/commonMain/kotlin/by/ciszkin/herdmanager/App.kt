package by.ciszkin.herdmanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.CheckForOllamaUpdateUseCase
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.util.setDefaultLocale
import org.koin.compose.koinInject
import by.ciszkin.herdmanager.presentation.components.AdaptiveScaffold
import by.ciszkin.herdmanager.presentation.components.NavigationItem
import by.ciszkin.herdmanager.presentation.modellist.ModelListScreen
import by.ciszkin.herdmanager.presentation.registry.RegistryScreen
import by.ciszkin.herdmanager.presentation.running.RunningScreen
import by.ciszkin.herdmanager.presentation.settings.SettingsScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition

@Composable
fun App() {
    val observeSettingsUseCase: ObserveSettingsUseCase = koinInject()
    val checkForOllamaUpdateUseCase: CheckForOllamaUpdateUseCase = koinInject()

    val settings by observeSettingsUseCase().collectAsState(initial = null)
    val updateInfo by checkForOllamaUpdateUseCase().collectAsState(initial = null)

    // Refresh update info at startup, throttled to once per day.
    LaunchedEffect(Unit) {
        checkForOllamaUpdateUseCase.refreshIfDue()
    }

    val language = settings?.language ?: "en"

    // CMP resources resolve from the JVM default locale (Locale.current), but
    // only when a composition re-reads it — resource states are cached on the
    // platform environment. Apply the language in two steps: set the default
    // locale, then key the whole UI on it so every resource resolves again.
    // Initial "en" guarantees the first observed (possibly persisted) language
    // always triggers an apply-and-rebuild pass.
    var appliedLanguage by remember { mutableStateOf("en") }
    LaunchedEffect(language) {
        if (appliedLanguage != language) {
            setDefaultLocale(language)
            appliedLanguage = language
        }
    }

    val isDarkTheme = when (settings?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }

    key(appliedLanguage) {
        MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
            var selectedRoute by remember { mutableStateOf(NavigationItem.Models.route) }

            AdaptiveScaffold(
                selectedRoute = selectedRoute,
                onRouteSelected = { selectedRoute = it },
                language = settings?.language ?: "en",
                hasUpdateBadge = updateInfo?.isNewerAvailable == true
            ) { contentModifier ->
                Surface(
                    modifier = contentModifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (selectedRoute) {
                        NavigationItem.Models.route -> {
                            Navigator(ModelListScreen) { navigator ->
                                SlideTransition(navigator)
                            }
                        }

                        NavigationItem.Registry.route -> {
                            Navigator(RegistryScreen) { navigator ->
                                SlideTransition(navigator)
                            }
                        }

                        NavigationItem.Running.route -> {
                            Navigator(RunningScreen) { navigator ->
                                SlideTransition(navigator)
                            }
                        }

                        NavigationItem.Settings.route -> {
                            Navigator(SettingsScreen) { navigator ->
                                SlideTransition(navigator)
                            }
                        }
                    }
                }
            }
        }
    }
}
