package by.ciszkin.herdmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.presentation.components.AdaptiveScaffold
import by.ciszkin.herdmanager.presentation.root.*

@Composable
fun App() {
    MaterialTheme {
        var currentRoute by remember { mutableStateOf("models") }

        AdaptiveScaffold(
            selectedRoute = currentRoute,
            onRouteSelected = { route -> currentRoute = route }
        ) { modifier ->
            Box(modifier = modifier.padding(16.dp).fillMaxSize()) {
                when (currentRoute) {
                    "models" -> ModelsScreen()
                    "registry" -> RegistryScreen()
                    "running" -> RunningScreen()
                    "settings" -> SettingsScreen()
                }
            }
        }
    }
}
