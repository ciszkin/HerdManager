package by.ciszkin.herdmanager

import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.usecase.ObserveSettingsUseCase
import by.ciszkin.herdmanager.presentation.settings.AvailableLanguage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

/**
 * Current activity's component name, used to recreate the activity after a
 * language change. Lives in the app module (not shared) since only
 * [MainActivity] reads/writes it.
 */
var mainActivityComponentName: ComponentName? = null

class MainActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        mainActivityComponentName = ComponentName(this, MainActivity::class.java)

        lifecycleScope.launch {
            val observeSettingsUseCase: ObserveSettingsUseCase by inject()
            val settings = observeSettingsUseCase()

            launch {
                settings
                    .map { it.themeMode }
                    .distinctUntilChanged()
                    .collect(::setSystemBarAppearance)
            }

            launch {
                settings
                    .map { it.language }
                    .distinctUntilChanged()
                    .drop(1)
                    .filter { getCurrentLocaleCode() != it }
                    .collect { recreateActivity() }
            }
        }

        setContent {
            App()
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

        if (VERSION.SDK_INT >= VERSION_CODES.R) {
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
    private fun recreateActivity() {
        try {
            val componentName = mainActivityComponentName
            if (componentName == null) {
                Log.w("MainActivity", "ComponentName not set. Cannot recreate activity.")
                return
            }
            val intent = Intent().apply {
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentLocaleCode(): String {
        val config = resources.configuration
        val locale = if (VERSION.SDK_INT >= VERSION_CODES.N) {
            config.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            config.locale ?: Locale.getDefault()
        }
        return AvailableLanguage.fromCode(locale.language).code
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
