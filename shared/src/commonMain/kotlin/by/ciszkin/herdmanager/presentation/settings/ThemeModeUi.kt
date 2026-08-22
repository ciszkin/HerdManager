package by.ciszkin.herdmanager.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import by.ciszkin.herdmanager.domain.model.ThemeMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Sun
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.theme_mode_dark
import herdmanager.shared.generated.resources.theme_mode_light
import herdmanager.shared.generated.resources.theme_mode_system
import org.jetbrains.compose.resources.stringResource

/**
 * Presentation-layer mapping for [ThemeMode] UI concerns (icon + label),
 * keeping the domain enum free of Compose dependencies.
 */
val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.LIGHT -> FeatherIcons.Sun
        ThemeMode.DARK -> FeatherIcons.Moon
        ThemeMode.SYSTEM -> FeatherIcons.Smartphone
    }

@Composable
fun ThemeMode.getLabel(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
    ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
    ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
}