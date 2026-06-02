package by.ciszkin.herdmanager.util

import by.ciszkin.herdmanager.domain.model.ThemeMode

actual fun recreateForThemeChange(previousTheme: ThemeMode, newTheme: ThemeMode) {
    // No-op on desktop - theme changes apply immediately
}
