package by.ciszkin.herdmanager.util

/**
 * Sets the JVM default locale, which Compose Multiplatform resources read when
 * resolving string resources (see CMP `ResourceEnvironment`). Must be called
 * before the composition that re-reads the resources is applied.
 */
expect fun setDefaultLocale(language: String)