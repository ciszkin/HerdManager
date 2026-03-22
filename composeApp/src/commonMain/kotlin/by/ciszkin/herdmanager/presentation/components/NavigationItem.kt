package by.ciszkin.herdmanager.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Package
import compose.icons.feathericons.Settings
import compose.icons.feathericons.PlayCircle

enum class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    Models("Models", FeatherIcons.Package, "models"),
    Registry("Registry", FeatherIcons.Globe, "registry"),
    Running("Running", FeatherIcons.PlayCircle, "running"),
    Settings("Settings", FeatherIcons.Settings, "settings")
}
