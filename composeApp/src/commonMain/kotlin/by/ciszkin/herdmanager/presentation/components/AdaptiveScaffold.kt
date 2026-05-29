package by.ciszkin.herdmanager.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AdaptiveScaffold(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    language: String,
    hasUpdateBadge: Boolean = false,
    content: @Composable (Modifier) -> Unit
)
