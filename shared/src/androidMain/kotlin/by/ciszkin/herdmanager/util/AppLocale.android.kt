package by.ciszkin.herdmanager.util

import java.util.Locale

actual fun setDefaultLocale(language: String) {
    Locale.setDefault(Locale(language))
}