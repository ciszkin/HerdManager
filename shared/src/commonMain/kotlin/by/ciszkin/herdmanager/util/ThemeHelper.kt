package by.ciszkin.herdmanager.util

import by.ciszkin.herdmanager.domain.model.ThemeMode

/**
 * Platform-specific function to trigger activity/theme recreation when theme mode changes.
 * On Android, this restarts the activity to ensure system bar appearance is updated correctly.
 * On Desktop (JVM), this is a no-op as theme changes apply immediately.
 */
expect fun recreateForThemeChange(previousTheme: ThemeMode, newTheme: ThemeMode)
