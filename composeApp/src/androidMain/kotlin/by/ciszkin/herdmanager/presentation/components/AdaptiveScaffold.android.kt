package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AdaptiveScaffold(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    language: String,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {},
        bottomBar = {
            NavigationBar {
                key(language) {
                    NavigationItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = selectedRoute == item.route,
                            onClick = { onRouteSelected(item.route) },
                            icon = {
                                Icon(imageVector = item.icon, contentDescription = item.getLabel())
                            },
                            label = {
                                Text(item.getLabel())
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}
