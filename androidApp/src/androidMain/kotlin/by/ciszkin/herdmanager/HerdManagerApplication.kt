package by.ciszkin.herdmanager

import android.app.Application
import by.ciszkin.herdmanager.data.connection.ConnectionManager
import by.ciszkin.herdmanager.di.initKoin
import by.ciszkin.herdmanager.di.localApplicationContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Custom Application class for HerdManager.
 * Initializes Koin DI and starts ConnectionManager once per process lifecycle.
 *
 * IMPORTANT: This class must be registered in AndroidManifest.xml:
 * ```xml
 * <application
 *     android:name=".HerdManagerApplication"
 *     ...>
 * ```
 */
class HerdManagerApplication : Application(), KoinComponent {

    private val connectionManager: ConnectionManager by inject()

    override fun onCreate() {
        super.onCreate()

        // Store application context for DI
        localApplicationContext = applicationContext

        // Initialize Koin DI
        initKoin {
            properties(mapOf("androidContext" to applicationContext))
        }

        // Start ConnectionManager for app lifetime
        connectionManager.start()
    }
}
