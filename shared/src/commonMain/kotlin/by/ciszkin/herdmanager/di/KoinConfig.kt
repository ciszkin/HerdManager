package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.di.core.coreModule
import by.ciszkin.herdmanager.di.network.networkModule
import by.ciszkin.herdmanager.di.repository.repositoryModule
import by.ciszkin.herdmanager.di.usecase.useCaseModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/**
 * Initialize Koin DI with the given modules and optional platform configuration.
 *
 * On Android, pass the application context via [properties] to provide fallback
 * for platform-specific dependencies:
 *
 * ```kotlin
 * initKoin {
 *     properties(put("androidContext", applicationContext))
 * }
 * ```
 *
 * On Desktop, no additional properties are needed.
 */
fun initKoin(
    platformConfig: KoinApplication.() -> Unit = {}
): KoinApplication = startKoin {
    modules(
        coreModule +
        networkModule +
        repositoryModule +
        useCaseModule
    )
    platformConfig()
}
