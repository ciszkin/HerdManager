package by.ciszkin.herdmanager.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Sun
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.theme_mode_dark
import herdmanager.composeapp.generated.resources.theme_mode_light
import herdmanager.composeapp.generated.resources.theme_mode_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class ThemeMode(
    val icon: ImageVector,
    val value: String,
    val labelRes: StringResource
) {
    LIGHT(FeatherIcons.Sun, "light", Res.string.theme_mode_light),
    DARK(FeatherIcons.Moon, "dark", Res.string.theme_mode_dark),
    SYSTEM(FeatherIcons.Smartphone, "system", Res.string.theme_mode_system);

    companion object {
        fun fromValue(value: String): ThemeMode {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }

    @Composable
    fun getLabel(): String = stringResource(labelRes)
}
