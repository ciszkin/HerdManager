package by.ciszkin.herdmanager.util

import android.content.Intent
import android.util.Log
import by.ciszkin.herdmanager.di.mainActivityComponentName
import by.ciszkin.herdmanager.di.provideApplicationContext
import by.ciszkin.herdmanager.domain.model.ThemeMode

actual fun recreateForThemeChange(previousTheme: ThemeMode, newTheme: ThemeMode) {
    if (previousTheme == newTheme) return
    try {
        val context = provideApplicationContext()
        val componentName = mainActivityComponentName
        if (componentName == null) {
            Log.w("ThemeHelper", "MainActivity ComponentName not set. Cannot recreate for theme change.")
            return
        }
        val intent = Intent().apply {
            component = componentName
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
