package by.ciszkin.herdmanager.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Package
import compose.icons.feathericons.Settings
import compose.icons.feathericons.PlayCircle
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.nav_models
import herdmanager.composeapp.generated.resources.nav_registry
import herdmanager.composeapp.generated.resources.nav_running
import herdmanager.composeapp.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class NavigationItem(
    val icon: ImageVector,
    val route: String,
    val labelRes: StringResource
) {
    Models(FeatherIcons.Package, "models", Res.string.nav_models),
    Registry(FeatherIcons.Globe, "registry", Res.string.nav_registry),
    Running(FeatherIcons.PlayCircle, "running", Res.string.nav_running),
    Settings(FeatherIcons.Settings, "settings", Res.string.nav_settings);

    @Composable
    fun getLabel(): String = stringResource(labelRes)
}
