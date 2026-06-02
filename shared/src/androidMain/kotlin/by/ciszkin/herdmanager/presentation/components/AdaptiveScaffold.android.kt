package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AdaptiveScaffold(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    language: String,
    hasUpdateBadge: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {},
        bottomBar = {
            NavigationBar {
                key(language) {
                    NavigationItem.entries.forEach { item ->
                        val showBadge = hasUpdateBadge && item.route == NavigationItem.Settings.route

                        NavigationBarItem(
                            selected = selectedRoute == item.route,
                            onClick = { onRouteSelected(item.route) },
                            icon = {
                                if (showBadge) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("!")
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = item.icon, contentDescription = item.getLabel())
                                    }
                                } else {
                                    Icon(imageVector = item.icon, contentDescription = item.getLabel())
                                }
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
