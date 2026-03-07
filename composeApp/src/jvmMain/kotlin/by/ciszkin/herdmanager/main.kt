package by.ciszkin.herdmanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.ThemeMode
import java.awt.Dimension
import java.util.Locale

fun main() {
    application {
        val settings by AppModule.observeSettingsUseCase().collectAsState(initial = null)
        val locale = when (settings?.language) {
            "be" -> Locale("be")
            else -> Locale("en")
        }
        LaunchedEffect(locale) {
            Locale.setDefault(locale)
        }

        val state = rememberWindowState(
            width = 1280.dp,
            height = 800.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )

        val isDarkTheme = when (settings?.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            else -> isSystemInDarkTheme()
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "HerdManager",
            state = state,
            resizable = true
        ) {
            window.minimumSize = Dimension(800, 600)

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    color = if (isDarkTheme) darkColorScheme().surfaceContainer else lightColorScheme().surfaceContainer
                ) {
                    App()
                }
            }
        }
    }
}