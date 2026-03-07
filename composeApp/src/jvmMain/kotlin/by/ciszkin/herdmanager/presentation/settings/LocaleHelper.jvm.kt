package by.ciszkin.herdmanager.presentation.settings

import java.util.Locale

actual fun setLocale(language: String) {
    val locale = when (language) {
        "be" -> Locale("be")
        else -> Locale("en")
    }
    Locale.setDefault(locale)
}
