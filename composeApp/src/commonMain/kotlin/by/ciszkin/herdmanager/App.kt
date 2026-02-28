package by.ciszkin.herdmanager

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    MaterialTheme {
        var selectedRoute by remember { mutableStateOf(NavigationItem.Models.route) }

        AdaptiveScaffold(
            selectedRoute = selectedRoute,
            onRouteSelected = { selectedRoute = it }
        ) { contentModifier ->
            Surface(modifier = contentModifier) {
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
