package by.ciszkin.herdmanager.domain.model

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String): ThemeMode {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}