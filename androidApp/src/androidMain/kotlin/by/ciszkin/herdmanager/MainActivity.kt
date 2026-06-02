package by.ciszkin.herdmanager

import android.content.ComponentName
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.di.localApplicationContext
import by.ciszkin.herdmanager.di.mainActivityComponentName
import by.ciszkin.herdmanager.di.provideApplicationContext
import by.ciszkin.herdmanager.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        localApplicationContext = applicationContext
        mainActivityComponentName = ComponentName(this, MainActivity::class.java)

        lifecycleScope.launch {
            val settings = AppModule.observeSettingsUseCase().first()
            setLocale(settings.language)
            setSystemBarAppearance(settings.themeMode)

            setContent {
                App()
            }
        }
    }

    private fun setSystemBarAppearance(themeMode: ThemeMode) {
        val isDarkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (isDarkTheme) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            val flags = decorView.systemUiVisibility
            decorView.systemUiVisibility = if (isDarkTheme) {
                flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun setLocale(language: String) {
        val locale = when (language) {
            "be" -> Locale("be")
            else -> Locale("en")
        }
        Locale.setDefault(locale)

        val context = provideApplicationContext()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
