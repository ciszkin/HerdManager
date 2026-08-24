package by.ciszkin.herdmanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.di.initKoin
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import org.koin.compose.koinInject
import java.awt.Dimension

fun main() {
    initKoin()

    application {
        val observeSettingsUseCase: ObserveSettingsUseCase = koinInject()
        val settings by observeSettingsUseCase().collectAsState(initial = null)

        val connectionManager: ConnectionManager = koinInject()
        LaunchedEffect(Unit) {
            connectionManager.start()
        }
        DisposableEffect(Unit) {
            onDispose {
                connectionManager.stop()
            }
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
