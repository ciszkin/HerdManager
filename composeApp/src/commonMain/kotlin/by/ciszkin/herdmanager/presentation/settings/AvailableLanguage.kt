package by.ciszkin.herdmanager.presentation.settings

import androidx.compose.runtime.Composable
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.language_belarusian
import herdmanager.composeapp.generated.resources.language_english
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class AvailableLanguage(
    val code: String,
    val labelRes: StringResource
) {
    ENGLISH("en", Res.string.language_english),
    BELARUSIAN("be", Res.string.language_belarusian);

    companion object {
        fun fromCode(code: String): AvailableLanguage = entries.find { it.code == code } ?: ENGLISH
    }

    @Composable
    fun getLabel(): String = stringResource(labelRes)
}
