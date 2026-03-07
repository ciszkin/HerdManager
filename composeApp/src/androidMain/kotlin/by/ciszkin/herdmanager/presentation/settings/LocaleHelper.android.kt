package by.ciszkin.herdmanager.presentation.settings

import android.content.res.Configuration
import by.ciszkin.herdmanager.data.local.applicationContext
import java.util.Locale

actual fun setLocale(language: String) {
    val locale = when (language) {
        "be" -> Locale("be")
        else -> Locale("en")
    }
    Locale.setDefault(locale)

    val context = applicationContext
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.createConfigurationContext(config)
}
