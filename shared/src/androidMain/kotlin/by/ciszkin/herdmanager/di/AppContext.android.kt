package by.ciszkin.herdmanager.di

import android.content.Context
import android.content.ComponentName
import org.koin.core.context.GlobalContext

/**
 * Android application context holder.
 *
 * This is set during [HerdManagerApplication.onCreate] before Koin initialization.
 * It provides platform-specific context for Android-only dependencies.
 */
lateinit var localApplicationContext: Context

/**
 * Main activity component name for system bar appearance management.
 */
var mainActivityComponentName: ComponentName? = null

/**
 * Provides the Android application context with fallback to Koin properties.
 *
 * Priority order:
 * 1. [localApplicationContext] (set during MainActivity initialization)
 * 2. Koin property "androidContext" (passed during initKoin)
 * 3. Throws [IllegalStateException] if neither is available
 *
 * @throws IllegalStateException if context is not initialized
 */
fun provideApplicationContext(): Context {
    if (::localApplicationContext.isInitialized) {
        return localApplicationContext
    }

    val koin = GlobalContext.get()
    val contextFromKoin = koin.getProperty<Context>("androidContext")
    if (contextFromKoin != null) {
        return contextFromKoin
    }

    throw IllegalStateException(
        "Application context not initialized. " +
            "Ensure HerdManagerApplication.onCreate() sets localApplicationContext before calling initKoin()."
    )
}
